package models.accounts.ms_teams.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.registration.RegistrationService
import models.behaviors.events.MSTeamsEventContext
import models.behaviors.{BotResult, BotResultService}
import models.behaviors.events.ms_teams.MSTeamsMessageEvent
import play.api.libs.ws.WSClient
import services.DataService
import services.caching.CacheService
import services.ms_teams.MSTeamsApiService
import services.ms_teams.apiModels.ActivityInfo
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class MSTeamsBotProfileTable(tag: Tag) extends Table[MSTeamsBotProfile](tag, "ms_teams_bot_profiles") {
  def teamId = column[String]("team_id")
  def tenantId = column[String]("tenant_id")
  def createdAt = column[OffsetDateTime]("created_at")
  def allowShortcutMention = column[Boolean]("allow_shortcut_mention")

  def * = (teamId, tenantId, createdAt, allowShortcutMention) <>
    ((MSTeamsBotProfile.apply _).tupled, MSTeamsBotProfile.unapply _)

}

class MSTeamsBotProfileServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             botResultServiceProvider: Provider[BotResultService],
                                             registrationServiceProvider: Provider[RegistrationService],
                                             cacheServiceProvider: Provider[CacheService],
                                             wsProvider: Provider[WSClient],
                                             apiServiceProvider: Provider[MSTeamsApiService],
                                             implicit val actorSystem: ActorSystem,
                                             implicit val ec: ExecutionContext
                                           ) extends MSTeamsBotProfileService {

  def dataService = dataServiceProvider.get
  def botResultService = botResultServiceProvider.get
  def registrationService = registrationServiceProvider.get
  def cacheService = cacheServiceProvider.get

  val all = TableQuery[MSTeamsBotProfileTable]

  def uncompiledAllForTenantIdQuery(tenantId: Rep[String]) = {
    all.filter(_.tenantId === tenantId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForTenantIdQuery _)

  def uncompiledFindQuery(tenantId: Rep[String]) = {
    uncompiledAllForTenantIdQuery(tenantId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(tenantId: String): Future[Option[MSTeamsBotProfile]] = {
    val action = findQuery(tenantId).result.map { r =>
      r.headOption
    }
    dataService.run(action)
  }

  def ensure(tenantId: String, teamName: String): Future[MSTeamsBotProfile] = {
    val query = findQuery(tenantId)
    val action = query.result.headOption.flatMap {
      case Some(existing) => {
        DBIO.successful(existing)
      }
      case None => {
        for {
          team <- registrationService.registerNewTeamAction(teamName)
          profile <- {
            val newProfile = MSTeamsBotProfile(
              team.id,
              tenantId,
              OffsetDateTime.now,
              allowShortcutMention = MSTeamsBotProfile.ALLOW_SHORTCUT_MENTION_DEFAULT
            )
            (all += newProfile).map { _ => newProfile }
          }
        } yield profile
      }
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
                              getEventualMaybeResult: MSTeamsMessageEvent => Future[Option[BotResult]],
                              botProfile: MSTeamsBotProfile,
                              info: ActivityInfo,
                              channelId: String,
                              userId: String,
                              originalMessageTs: String,
                              maybeThreadTs: Option[String],
                              isEphemeral: Boolean,
                              beQuiet: Boolean
                            ): Future[Option[String]] = {
    val event = MSTeamsMessageEvent(
      MSTeamsEventContext(
        botProfile,
        info
      ),
      "",
      None,
      isUninterruptedConversation = false,
      isEphemeral,
      Some(info.responseUrl),
      beQuiet
    )
    sendResult(getEventualMaybeResult(event))
  }

}
