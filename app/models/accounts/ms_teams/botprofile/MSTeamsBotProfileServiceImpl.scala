package models.accounts.ms_teams.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.registration.RegistrationService
import models.accounts.user.User
import models.behaviors.events.{EventType, SlackEventContext, SlackMessage, SlackMessageEvent}
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

class MSTeamsBotProfileTable(tag: Tag) extends Table[MSTeamsBotProfile](tag, "ms_teams_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def msTeamsOrgId = column[String]("ms_teams_org_id")
  def token = column[String]("token")
  def expiresAt = column[OffsetDateTime]("expires_at")
  def refreshToken = column[String]("refresh_token")
  def createdAt = column[OffsetDateTime]("created_at")
  def allowShortcutMention = column[Boolean]("allow_shortcut_mention")

  def * = (userId, teamId, msTeamsOrgId, token, expiresAt, refreshToken, createdAt, allowShortcutMention) <>
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

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def uncompiledFindQuery(userId: Rep[String], msTeamsOrgId: Rep[String]) = {
    uncompiledAllForUserIdQuery(userId).filter(_.msTeamsOrgId === msTeamsOrgId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def ensure(
              userId: String,
              msTeamsOrgId: String,
              msTeamsOrgName: String,
              token: String,
              expiresAt: OffsetDateTime,
              refreshToken: String
            ): Future[MSTeamsBotProfile] = {
    val query = findQuery(userId, msTeamsOrgId)
    val action = query.result.headOption.flatMap {
      case Some(existing) => {
        val profile = MSTeamsBotProfile(userId, existing.teamId, msTeamsOrgId, token, expiresAt, refreshToken, existing.createdAt, existing.allowShortcutMention)
        for {
          maybeTeam <- DBIO.from(dataService.teams.find(existing.teamId))
          _ <- query.update(profile)
          _ <- maybeTeam.map { team =>
            DBIO.from(dataService.teams.setNameFor(team, msTeamsOrgName))
          }.getOrElse(DBIO.successful(Unit))
        } yield profile
      }
      case None => {
        for {
          maybeExistingTeamId <- allForUserIdQuery(userId).result.map(_.headOption.map(_.teamId))
          maybeExistingTeam <- maybeExistingTeamId.map { existingTeamId =>
            dataService.teams.findAction(existingTeamId)
          }.getOrElse(DBIO.successful(None))
          team <- maybeExistingTeam.map(DBIO.successful).getOrElse(registrationService.registerNewTeamAction(msTeamsOrgName))
          existingTeamSlackBotProfiles <- dataService.slackBotProfiles.allForAction(team)
          profile <- {
            val newProfile = MSTeamsBotProfile(
              userId,
              team.id,
              msTeamsOrgId,
              token,
              expiresAt,
              refreshToken,
              OffsetDateTime.now,
              allowShortcutMention = existingTeamSlackBotProfiles.headOption.map(_.allowShortcutMention).getOrElse {
                MSTeamsBotProfile.ALLOW_SHORTCUT_MENTION_DEFAULT
              }
            )
            (all += newProfile).map { _ => newProfile }
          }
        } yield profile
      }
    }
    dataService.run(action)
  }

}
