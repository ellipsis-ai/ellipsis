package models.accounts.slack.botprofile

import java.time.OffsetDateTime
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import models.behaviors.BotResult
import models.behaviors.events.SlackMessageEvent
import models.team.Team
import play.api.Logger
import services.{DataService, SlackEventService}
import utils.{SlackMessageReactionHandler, SlackTimestamp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
                                          implicit val actorSystem: ActorSystem
                                        ) extends SlackBotProfileService {

  def dataService = dataServiceProvider.get
  def slackEventService = slackEventServiceProvider.get

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
            DBIO.from(dataService.teams.setInitialNameFor(team, slackTeamName))
          }.getOrElse(DBIO.successful(Unit))
        } yield profile
      }
      case None => DBIO.from(dataService.teams.create(slackTeamName)).flatMap { team =>
        val newProfile = SlackBotProfile(userId, team.id, slackTeamId, token, OffsetDateTime.now)
        (all += newProfile).map { _ => newProfile }
      }
    }
    dataService.run(action)
  }

  def eventualMaybeEvent(slackTeamId: String, channelId: String, userId: String): Future[Option[SlackMessageEvent]] = {
    allForSlackTeamId(slackTeamId).map { botProfiles =>
      botProfiles.headOption.map { botProfile =>
        SlackMessageEvent(botProfile, channelId, None, userId, "", SlackTimestamp.now, slackEventService.clientFor(botProfile))
      }
    }
  }

  private def sendResult(eventualMaybeResult: Future[Option[BotResult]]): Future[Option[String]] = {
    for {
      maybeResult <- eventualMaybeResult
      maybeTimestamp <- maybeResult.map { result =>
        result.sendIn(None, dataService)
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
      maybeEvent <- eventualMaybeEvent(slackTeamId, channelId, userId)
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
