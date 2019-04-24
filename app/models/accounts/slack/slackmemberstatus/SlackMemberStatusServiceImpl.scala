package models.accounts.slack.slackmemberstatus

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.IDs
import models.accounts.registration.RegistrationService
import models.accounts.slack.SlackProvider
import models.behaviors.BotResultService
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
                              firstObservedAt: OffsetDateTime
                            )

class SlackMemberStatusTable(tag: Tag) extends Table[SlackMemberStatus](tag, "slack_member_statuses") {
  def id = column[String]("id", O.PrimaryKey)
  def slackTeamId = column[String]("slack_team_id")
  def slackUserId = column[String]("slack_user_id")
  def isDeleted = column[Boolean]("is_deleted")
  def firstObservedAt = column[OffsetDateTime]("first_observed_at")

  def * = (id, slackTeamId, slackUserId, isDeleted, firstObservedAt) <> ((SlackMemberStatus.apply _).tupled, SlackMemberStatus.unapply _)

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

  private def newStatusFor(membershipData: MembershipData) = SlackMemberStatus(
    IDs.next,
    membershipData.team_id,
    membershipData.id,
    isDeleted = membershipData.deleted,
    firstObservedAt = membershipData.lastUpdated
  )

  def updateFor(membershipData: MembershipData): Future[Unit] = {
    val action = for {
      maybeMostRecent <- mostRecentQuery(membershipData.team_id, membershipData.id).result.map(_.headOption)
      maybeNewStatusToAdd <- DBIO.successful(maybeMostRecent.flatMap { status =>
        if (status.isDeleted == membershipData.deleted) {
          None
        } else {
          Some(newStatusFor(membershipData))
        }
      }.orElse {
        Some(newStatusFor(membershipData))
      })
      maybeWithEarlierTimestampToUse <- maybeNewStatusToAdd.map { status =>
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
          maybeEarliestTimestamp.map { earliestTimestamp =>
            if (earliestTimestamp.isBefore(status.firstObservedAt)) {
              status.copy(firstObservedAt = earliestTimestamp)
            } else {
              status
            }
          }
        }
      }.getOrElse(DBIO.successful(None))
      _ <- maybeWithEarlierTimestampToUse.map(s => all += s).getOrElse(DBIO.successful({}))
    } yield {}
    dataService.run(action)
  }

}