package models.accounts.slack.slackmemberstatus

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.IDs
import models.accounts.registration.RegistrationService
import models.accounts.slack.SlackProvider
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import play.api.Logger
import play.api.libs.ws.WSClient
import services.DataService
import services.caching.CacheService
import services.slack._
import services.slack.apiModels.MembershipData

import scala.concurrent.{ExecutionContext, Future}

case class SlackMemberStatus(
                              id: String,
                              slackTeamId: String,
                              slackUserId: String,
                              isDeleted: Boolean,
                              isBotOrApp: Boolean,
                              firstObservedAt: OffsetDateTime
                            )

class SlackMemberStatusTable(tag: Tag) extends Table[SlackMemberStatus](tag, "slack_member_statuses") {
  def id = column[String]("id", O.PrimaryKey)
  def slackTeamId = column[String]("slack_team_id")
  def slackUserId = column[String]("slack_user_id")
  def isDeleted = column[Boolean]("is_deleted")
  def isBotOrApp = column[Boolean]("is_bot_or_app")
  def firstObservedAt = column[OffsetDateTime]("first_observed_at")

  def * = (id, slackTeamId, slackUserId, isDeleted, isBotOrApp, firstObservedAt) <> ((SlackMemberStatus.apply _).tupled, SlackMemberStatus.unapply _)

}

class SlackMemberStatusServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             slackEventServiceProvider: Provider[SlackEventService],
                                             botResultServiceProvider: Provider[BotResultService],
                                             registrationServiceProvider: Provider[RegistrationService],
                                             cacheServiceProvider: Provider[CacheService],
                                             wsProvider: Provider[WSClient],
                                             slackApiServiceProvider: Provider[SlackApiService],
                                             implicit val actorSystem: ActorSystem,
                                             implicit val ec: ExecutionContext
                                           ) extends SlackMemberStatusService {

  def dataService = dataServiceProvider.get
  def slackEventService = slackEventServiceProvider.get
  def botResultService = botResultServiceProvider.get
  def registrationService = registrationServiceProvider.get
  def cacheService = cacheServiceProvider.get
  def slackApiService = slackApiServiceProvider.get

  val all = TableQuery[SlackMemberStatusTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledMostRecentQuery(slackTeamId: Rep[String], slackUserId: Rep[String]) = {
    all.
      filter(_.slackTeamId === slackTeamId).
      filter(_.slackUserId === slackUserId).
      sortBy(_.firstObservedAt.desc).
      take(1)
  }
  val mostRecentQuery = Compiled(uncompiledMostRecentQuery _)

  def uncompiledAllForTeamQuery(slackTeamId: Rep[String]) = {
    all.filter(_.slackTeamId === slackTeamId)
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  private def newStatusFor(membershipData: MembershipData) = SlackMemberStatus(
    IDs.next,
    membershipData.team_id,
    membershipData.id,
    isDeleted = membershipData.deleted,
    isBotOrApp = (membershipData.is_bot || membershipData.is_app_user || membershipData.id == "USLACKBOT"),
    firstObservedAt = membershipData.lastUpdated
  )

  private def updateFor(membershipData: MembershipData): Future[Unit] = {
    val action = for {
      maybeMostRecent <- mostRecentQuery(membershipData.team_id, membershipData.id).result.map(_.headOption)
      maybeUpdatedStatusToAdd <- DBIO.successful(maybeMostRecent.flatMap { status =>
        if (status.isDeleted == membershipData.deleted) {
          None
        } else {
          Some(newStatusFor(membershipData))
        }
      })
      maybeFirstStatusToAdd <- DBIO.successful(
        if (maybeMostRecent.isEmpty) {
          Some(newStatusFor(membershipData))
        } else {
          None
        }
      )
      maybeFirstWithEarlierTimestampToUse <- maybeFirstStatusToAdd.map { status =>
        for {
          linkedAccounts <- dataService.linkedAccounts.allForLoginInfoAction(LoginInfo(SlackProvider.ID, status.slackUserId))
          withSlackTeamIds <- DBIO.sequence(linkedAccounts.map { ea =>
            for {
              maybeTeam <- dataService.teams.findAction(ea.user.teamId)
              botProfiles <- maybeTeam.map { team =>
                dataService.slackBotProfiles.allForAction(team)
              }.getOrElse(DBIO.successful(Seq()))
            } yield {
              (ea, botProfiles.map(_.slackTeamId))
            }
          })
        } yield {
          val maybeEarliestTimestamp = withSlackTeamIds.
            filter { case(_, slackTeamIds) => slackTeamIds.contains(membershipData.team_id) }.
            map { case(linked, _) => linked.createdAt }.
            sorted.
            headOption
          maybeEarliestTimestamp.flatMap { earliestTimestamp =>
            if (earliestTimestamp.isBefore(status.firstObservedAt)) {
              Some(status.copy(firstObservedAt = earliestTimestamp))
            } else {
              None
            }
          }.orElse(Some(status))
        }
      }.getOrElse(DBIO.successful(None))
      maybeStatusToAdd <- DBIO.successful(maybeUpdatedStatusToAdd.orElse(maybeFirstWithEarlierTimestampToUse))
      _ <- maybeStatusToAdd.map(s => all += s).getOrElse(DBIO.successful({}))
    } yield {}
    dataService.run(action)
  }

  private def logIgnorableSlackApiErrorMessageFor(errorCode: String, botProfile: SlackBotProfile): Unit = {
    Logger.info(s"${errorCode} trying to update slack memberships for team ${botProfile.teamId}")
  }

  private def logErrorMessageFor(t: Throwable, botProfile: SlackBotProfile): Unit = {
    Logger.error(s"Error trying to update slack memberships for team ${botProfile.teamId}:\n${t.getMessage}")
  }

  private def updateAllForProfile(botProfile: SlackBotProfile): Future[Unit] = {
    val client = slackApiService.clientFor(botProfile)
    (for {
      members <- client.allUsers()
      _ <- Future.sequence(members.map { ea =>
        updateFor(ea)
      })
    } yield {}).recover {
      case SlackApiError(code) if code == "invalid_auth" => logIgnorableSlackApiErrorMessageFor(code, botProfile)
      case SlackApiError(code) if code == "account_inactive" => logIgnorableSlackApiErrorMessageFor(code, botProfile)
      case SlackApiError(code) if code == "token_revoked" => logIgnorableSlackApiErrorMessageFor(code, botProfile)
      case t: Throwable => logErrorMessageFor(t, botProfile)
    }
  }

  def hasRunAlreadyToday: Future[Boolean] = {
    cacheService.get[OffsetDateTime](lastRunKey).map { maybeLastRun =>
      maybeLastRun.exists(_.toLocalDate == OffsetDateTime.now.toLocalDate)
    }
  }

  def updateAllForProfiles(profiles: Seq[SlackBotProfile]): Future[Unit] = {
    profiles.headOption.map { profile =>
      updateAllForProfile(profile).flatMap(_ => updateAllForProfiles(profiles.tail))
    }.getOrElse {
      Future.successful({})
    }
  }

  def updateAll: Future[Unit] = {
    hasRunAlreadyToday.flatMap { hasRun =>
      if (hasRun) {
        Logger.info("Slack membership update already ran today")
        Future.successful({})
      } else {
        Logger.info("Slack membership update running…")
        for {
          _ <- cacheService.set(lastRunKey, OffsetDateTime.now)
          profiles <- dataService.slackBotProfiles.allProfiles
          _ <- updateAllForProfiles(profiles)
        } yield {}
      }
    }
  }

  def allFor(slackTeamId: String): Future[Seq[SlackMemberStatus]] = {
    val action = allForTeamQuery(slackTeamId).result
    dataService.run(action)
  }

}
