package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.registration.RegistrationService
import models.accounts.user.User
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
import models.behaviors.events.{EventType, SlackEventContext}
import models.behaviors.{BotResult, BotResultService}
import models.team.Team
import play.api.Logger
import play.api.libs.ws.WSClient
import services.caching.CacheService
import services.slack._
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

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def uncompiledFindQuery(userId: Rep[String], slackTeamId: Rep[String]) = {
    uncompiledAllForUserIdQuery(userId).filter(_.slackTeamId === slackTeamId)
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

  def maybeFirstForAction(team: Team, user: User): DBIO[Option[SlackBotProfile]] = {
    for {
      botProfiles <- allForAction(team)
      maybeUserProfiles <- DBIO.from(dataService.users.maybeSlackTeamIdsFor(user))
    } yield {
      botProfiles.find { botProfile =>
        maybeUserProfiles.exists(_.contains(botProfile.slackTeamId))
      }.orElse(botProfiles.headOption)
    }
  }

  def maybeFirstFor(team: Team, user: User): Future[Option[SlackBotProfile]] = {
    dataService.run(maybeFirstForAction(team, user))
  }

  def uncompiledAllForSlackTeamQuery(slackTeamId: Rep[String]) = {
    all.filter(_.slackTeamId === slackTeamId)
  }
  val allForSlackTeamQuery = Compiled(uncompiledAllForSlackTeamQuery _)

  def allForSlackTeamIdAction(slackTeamId: String): DBIO[Seq[SlackBotProfile]] = {
    allForSlackTeamQuery(slackTeamId).result
  }

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]] = {
    dataService.run(allForSlackTeamIdAction(slackTeamId))
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
    val query = findQuery(userId, slackTeamId)
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
        for {
          maybeExistingTeamId <- allForUserIdQuery(userId).result.map(_.headOption.map(_.teamId))
          maybeExistingTeam <- maybeExistingTeamId.map { existingTeamId =>
            dataService.teams.findAction(existingTeamId)
          }.getOrElse(DBIO.successful(None))
          team <- maybeExistingTeam.map(DBIO.successful).getOrElse(registrationService.registerNewTeamAction(slackTeamName))
          existingTeamSlackBotProfiles <- dataService.slackBotProfiles.allForAction(team)
          profile <- {
            val newProfile = SlackBotProfile(
              userId,
              team.id,
              slackTeamId,
              token,
              OffsetDateTime.now,
              allowShortcutMention = existingTeamSlackBotProfiles.headOption.map(_.allowShortcutMention).getOrElse {
                SlackBotProfile.ALLOW_SHORTCUT_MENTION_DEFAULT
              }
            )
            (all += newProfile).map { _ => newProfile }
          }
        } yield profile
      }
    }
    dataService.run(action)
  }

  def eventualMaybeEvent(
                          slackTeamId: String,
                          channelId: String,
                          maybeUserId: Option[String],
                          maybeOriginalEventType: Option[EventType]
                        ): Future[Option[SlackMessageEvent]] = {
    allForSlackTeamId(slackTeamId).map { botProfiles =>
      botProfiles.headOption.map { botProfile =>
        // TODO: Create a new class for placeholder events
        // https://github.com/ellipsis-ai/ellipsis/issues/1719
        SlackMessageEvent(
          SlackEventContext(
            botProfile,
            channelId,
            None,
            maybeUserId.getOrElse(botProfile.userId)
          ),
          SlackMessage.blank,
          None,
          None,
          maybeOriginalEventType,
          maybeScheduled = None,
          isUninterruptedConversation = false,
          isEphemeral = false,
          maybeResponseUrl = None,
          beQuiet = false
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

  def maybeNameForAction(botProfile: SlackBotProfile): DBIO[Option[String]] = {
    val teamId = botProfile.teamId
    slackEventService.maybeSlackUserDataForAction(botProfile).flatMap { maybeSlackUserData =>
      maybeSlackUserData.map { slackUserData =>
        val name = slackUserData.getDisplayName
        cacheService.cacheBotNameAction(name, teamId).map { _ =>
          Some(name)
        }
      }.getOrElse {
        Logger.error(s"No bot user data returned from Slack API for ${botProfile.botDebugInfo}; using fallback cache")
        cacheService.getBotNameAction(teamId)
      }
    }
  }

  def maybeNameFor(botProfile: SlackBotProfile): Future[Option[String]] = {
    dataService.run(maybeNameForAction(botProfile))
  }

  // TODO: this might need to be at the team level for enterprise grid cases
  def toggleMentionShortcut(botProfile: SlackBotProfile, enableShortcut: Boolean): Future[Option[Boolean]] = {
    val query = findQuery(botProfile.userId, botProfile.slackTeamId)
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
                              botProfile: SlackBotProfile,
                              channelId: String,
                              slackUserId: String,
                              originalMessageTs: String,
                              maybeOriginalEventType: Option[EventType],
                              maybeThreadTs: Option[String],
                              isEphemeral: Boolean,
                              maybeResponseUrl: Option[String],
                              beQuiet: Boolean
  ): Future[Option[String]] = {
    val delayMilliseconds = 1000
    val event = syntheticMessageEvent(botProfile, channelId, originalMessageTs, maybeThreadTs, slackUserId, maybeOriginalEventType, isEphemeral, maybeResponseUrl, beQuiet)
    val eventualResult = sendResult(getEventualMaybeResult(event))
    SlackMessageReactionHandler.handle(
      slackApiService.clientFor(botProfile),
      eventualResult,
      channelId,
      originalMessageTs,
      delayMilliseconds
    )
    eventualResult
  }
}
