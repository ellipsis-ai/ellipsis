package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.registration.RegistrationService
import models.behaviors.events.{EventType, SlackMessage, SlackMessageEvent}
import models.behaviors.{BotResult, BotResultService}
import models.team.Team
import play.api.Logger
import play.api.libs.ws.WSClient
import services.caching.CacheService
import services.slack.{MalformedResponseException, SlackApiService, SlackEventService}
import services.DataService
import slick.dbio.DBIO
import utils._

import scala.concurrent.{ExecutionContext, Future}

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def slackTeamId = column[String]("slack_team_id")
  def token = column[String]("token")
  def createdAt = column[OffsetDateTime]("created_at")
  def allowShortcutMention = column[Boolean]("allow_shortcut_mention")

  def * = (userId, teamId, slackTeamId, token, createdAt, allowShortcutMention) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

}

class SlackBotProfileServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             slackEventServiceProvider: Provider[SlackEventService],
                                             botResultServiceProvider: Provider[BotResultService],
                                             registrationServiceProvider: Provider[RegistrationService],
                                             cacheServiceProvider: Provider[CacheService],
                                             wsProvider: Provider[WSClient],
                                             slackApiServiceProvider: Provider[SlackApiService],
                                             implicit val actorSystem: ActorSystem,
                                             implicit val ec: ExecutionContext
                                        ) extends SlackBotProfileService {

  def dataService = dataServiceProvider.get
  def slackEventService = slackEventServiceProvider.get
  def botResultService = botResultServiceProvider.get
  def registrationService = registrationServiceProvider.get
  def cacheService = cacheServiceProvider.get
  def slackApiService = slackApiServiceProvider.get

  val all = TableQuery[SlackBotProfileTable]

  def allProfiles: Future[Seq[SlackBotProfile]] = dataService.run(all.result)

  def uncompiledFindQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForAction(team: Team): DBIO[Seq[SlackBotProfile]] = {
    allForTeamQuery(team.id).result
  }

  def allFor(team: Team): Future[Seq[SlackBotProfile]] = {
    dataService.run(allForAction(team))
  }

  def uncompiledAllForSlackTeamQuery(slackTeamId: Rep[String]) = {
    all.filter(_.slackTeamId === slackTeamId)
  }
  val allForSlackTeamQuery = Compiled(uncompiledAllForSlackTeamQuery _)

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]] = {
    dataService.run(allForSlackTeamQuery(slackTeamId).result)
  }

  def admin: Future[SlackBotProfile] = allForSlackTeamId(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID).map(_.head)

  def uncompiledAllSinceQuery(when: Rep[OffsetDateTime]) = {
    all.filter(_.createdAt >= when)
  }
  val allSinceQuery = Compiled(uncompiledAllSinceQuery _)

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]] = {
    dataService.run(allSinceQuery(when).result)
  }

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile] = {
    val query = findQuery(userId)
    val action = query.result.headOption.flatMap {
      case Some(existing) => {
        val profile = SlackBotProfile(userId, existing.teamId, slackTeamId, token, existing.createdAt, existing.allowShortcutMention)
        for {
          maybeTeam <- DBIO.from(dataService.teams.find(existing.teamId))
          _ <- query.update(profile)
          _ <- maybeTeam.map { team =>
            DBIO.from(dataService.teams.setNameFor(team, slackTeamName))
          }.getOrElse(DBIO.successful(Unit))
        } yield profile
      }
      case None => {
        DBIO.from(registrationService.registerNewTeam(slackTeamName)).flatMap { team =>
          val newProfile = SlackBotProfile(
            userId,
            team.id,
            slackTeamId,
            token,
            OffsetDateTime.now,
            allowShortcutMention = SlackBotProfile.ALLOW_SHORTCUT_MENTION_DEFAULT
          )
          (all += newProfile).map { _ => newProfile }
        }
      }
    }
    dataService.run(action)
  }

  def eventualMaybeEvent(slackTeamId: String, channelId: String, maybeUserId: Option[String], maybeOriginalEventType: Option[EventType]): Future[Option[SlackMessageEvent]] = {
    allForSlackTeamId(slackTeamId).map { botProfiles =>
      botProfiles.headOption.map { botProfile =>
        // TODO: Create a new class for placeholder events
        // https://github.com/ellipsis-ai/ellipsis/issues/1719
        SlackMessageEvent(
          botProfile,
          slackTeamId,
          channelId,
          None,
          maybeUserId.getOrElse(botProfile.userId),
          SlackMessage.blank,
          None,
          SlackTimestamp.now,
          maybeOriginalEventType,
          isUninterruptedConversation = false,
          isEphemeral = false
        )
      }
    }
  }

  def channelsFor(botProfile: SlackBotProfile): SlackChannels = {
    SlackChannels(slackApiService.clientFor(botProfile))
  }

  def maybeNameFor(slackTeamId: String): Future[Option[String]] = {
    for {
      maybeSlackBotProfile <- allForSlackTeamId(slackTeamId).map(_.headOption)
      maybeName <- maybeSlackBotProfile.map { slackBotProfile =>
        maybeNameFor(slackBotProfile)
      }.getOrElse(Future.successful(None))
    } yield maybeName
  }

  def maybeNameFor(botProfile: SlackBotProfile): Future[Option[String]] = {
    val teamId = botProfile.teamId
    slackEventService.maybeSlackUserDataFor(botProfile).map { maybeSlackUserData =>
      maybeSlackUserData.map { slackUserData =>
        val name = slackUserData.getDisplayName
        cacheService.cacheBotName(name, teamId)
        name
      }.orElse {
        Logger.error("No bot user data returned from Slack API; using fallback cache")
        cacheService.getBotName(teamId)
      }
    }.recover {
      case e: MalformedResponseException => {
        Logger.warn("Couldn’t retrieve bot user data from Slack API because of an invalid response; using fallback cache", e)
        cacheService.getBotName(teamId)
      }
    }
  }

  def toggleMentionShortcut(botProfile: SlackBotProfile, enableShortcut: Boolean): Future[Option[Boolean]] = {
    val query = findQuery(botProfile.userId)
    val action = query.result.headOption.flatMap {
      case Some(existing) => {
        for {
          _ <- query.update(existing.copy(allowShortcutMention = enableShortcut))
        } yield {
          Some(enableShortcut)
        }
      }
      case None => DBIO.successful(None)
    }
    dataService.run(action)
  }

  private def sendResult(eventualMaybeResult: Future[Option[BotResult]]): Future[Option[String]] = {
    for {
      maybeResult <- eventualMaybeResult
      maybeTimestamp <- maybeResult.map { result =>
        botResultService.sendIn(result, None)
      }.getOrElse(Future.successful(None))
    } yield maybeTimestamp
  }

  def sendResultWithNewEvent(
                              description: String,
                              getEventualMaybeResult: SlackMessageEvent => Future[Option[BotResult]],
                              userSlackTeamId: String,
                              botProfile: SlackBotProfile,
                              channelId: String,
                              userId: String,
                              originalMessageTs: String,
                              maybeThreadTs: Option[String],
                              isEphemeral: Boolean
  ): Future[Option[String]] = {
    val delayMilliseconds = 1000
    val event = SlackMessageEvent(
      botProfile,
      userSlackTeamId,
      channelId,
      maybeThreadTs,
      userId,
      SlackMessage.blank,
      None,
      SlackTimestamp.now,
      None,
      isUninterruptedConversation = false,
      isEphemeral
    )
    val eventualResult = sendResult(getEventualMaybeResult(event))
    SlackMessageReactionHandler.handle(
      slackApiService.clientFor(botProfile),
      eventualResult,
      channelId,
      originalMessageTs,
      delayMilliseconds
    )
    eventualResult.recover {
      case e: BotNotInSlackChannelException => throw e.copy(maybeEvent = Some(event))
    }
  }
}
