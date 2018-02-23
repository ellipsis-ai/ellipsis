package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import javax.inject.{Inject, Provider}
import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import models.accounts.registration.RegistrationService
import models.behaviors.{BotResult, BotResultService}
import models.behaviors.events.{EventType, SlackMessage, SlackMessageEvent}
import models.team.Team
import play.api.Logger
import services.{CacheService, DataService, SlackEventService}
import slack.api.SlackApiClient
import slick.dbio.DBIO
import utils.{SlackChannels, SlackMessageReactionHandler, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def slackTeamId = column[String]("slack_team_id")
  def token = column[String]("token")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (userId, teamId, slackTeamId, token, createdAt) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

}

class SlackBotProfileServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             slackEventServiceProvider: Provider[SlackEventService],
                                             botResultServiceProvider: Provider[BotResultService],
                                             registrationServiceProvider: Provider[RegistrationService],
                                             cacheServiceProvider: Provider[CacheService],
                                             implicit val actorSystem: ActorSystem,
                                             implicit val ec: ExecutionContext
                                        ) extends SlackBotProfileService {

  def dataService = dataServiceProvider.get
  def slackEventService = slackEventServiceProvider.get
  def botResultService = botResultServiceProvider.get
  def registrationService = registrationServiceProvider.get
  def cacheService = cacheServiceProvider.get

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
        val profile = SlackBotProfile(userId, existing.teamId, slackTeamId, token, existing.createdAt)
        for {
          maybeTeam <- DBIO.from(dataService.teams.find(existing.teamId))
          _ <- query.update(profile)
          _ <- maybeTeam.map { team =>
            DBIO.from(dataService.teams.setNameFor(team, slackTeamName))
          }.getOrElse(DBIO.successful(Unit))
        } yield profile
      }
      case None => DBIO.from(registrationService.registerNewTeam(slackTeamName)).flatMap { team =>
        val newProfile = SlackBotProfile(userId, team.id, slackTeamId, token, OffsetDateTime.now)
        (all += newProfile).map { _ => newProfile }
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
          channelId,
          None,
          maybeUserId.getOrElse(botProfile.userId),
          SlackMessage.blank,
          None,
          SlackTimestamp.now,
          slackEventService.clientFor(botProfile),
          maybeOriginalEventType
        )
      }
    }
  }

  def channelsFor(botProfile: SlackBotProfile, cacheService: CacheService): SlackChannels = {
    SlackChannels(SlackApiClient(botProfile.token), cacheService, botProfile.slackTeamId)
  }

  def maybeNameForAction(botProfile: SlackBotProfile): DBIO[Option[String]] = {
    val teamId = botProfile.teamId
    DBIO.from(slackEventService.maybeSlackUserDataFor(botProfile).map { maybeSlackUserData =>
      maybeSlackUserData.map { slackUserData =>
        val name = slackUserData.getDisplayName
        cacheService.cacheBotName(name, teamId)
        name
      }.orElse {
        Logger.error("No bot user data returned from Slack API; using fallback cache")
        cacheService.getBotName(teamId)
      }
    }.recover {
      case e: slack.api.InvalidResponseError => {
        Logger.warn("Couldn’t retrieve bot user data from Slack API because of an invalid response; using fallback cache", e)
        cacheService.getBotName(teamId)
      }
    })
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
    slackTeamId: String,
    channelId: String,
    userId: String,
    originalMessageTs: String
  ): Future[Unit] = {
    val delayMilliseconds = 1000
    (for {
      maybeEvent <- eventualMaybeEvent(slackTeamId, channelId, Some(userId), None)
      _ <- maybeEvent.map { event =>
        val eventualResult = getEventualMaybeResult(event)
        sendResult(eventualResult)
        SlackMessageReactionHandler.handle(event.client, eventualResult, channelId, originalMessageTs, delayMilliseconds)
      }.getOrElse(Future.successful(None))
    } yield {}).recover {
      case t: Throwable => {
        Logger.error(s"Exception responding to a Slack action: $description", t)
      }
    }
  }
}
