package controllers.admin


import java.time.OffsetDateTime

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import javax.inject.Inject
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import services.slack.SlackApiService
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class SlackUsersController @Inject() (
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val dataService: DataService,
                                      val lambdaService: AWSLambdaService,
                                      val configuration: Configuration,
                                      val assetsProvider: Provider[RemoteAssets],
                                      val slackApiService: SlackApiService,
                                      implicit val ec: ExecutionContext
                                    ) extends AdminAuth {

  def list(teamId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
        for {
          maybeTeam <- dataService.teams.find(teamId)
          botProfiles <- maybeTeam.map { team =>
            dataService.slackBotProfiles.allFor(team)
          }.getOrElse(Future.successful(Seq()))
          members <- Future.sequence(botProfiles.map { profile =>
            slackApiService.clientFor(profile).allUsers()
          }).map(_.flatten.sortBy(_.lastUpdated).reverse)
        } yield {
          val (botsAndApps, users) = members.partition(ea => ea.is_app_user || ea.is_bot)
          val (deletedUsers, activeUsers) = users.partition(_.deleted)
          val (deletedBotsAndApps, activeBotsAndApps) = botsAndApps.partition(_.deleted)
          Ok(views.html.admin.slackUsers.grouped(viewConfig(None), activeUsers, deletedUsers, activeBotsAndApps, deletedBotsAndApps))
        }
    })
  }

  case class MonthData(monthStart: OffsetDateTime, activeCount: Int, inactiveCount: Int)

  implicit val monthDataFormat = Json.format[MonthData]

  private def startOfMonthFor(when: OffsetDateTime): OffsetDateTime = {
    when.withDayOfMonth(1).withHour(0).withMinute(0).withNano(0)
  }

  def activeUsersByMonth(teamId: String)= silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        maybeTeam <- dataService.teams.find(teamId)
        botProfiles <- maybeTeam.map { team =>
          dataService.slackBotProfiles.allFor(team)
        }.getOrElse(Future.successful(Seq()))
        data <- Future.sequence(botProfiles.map { profile =>
          dataService.slackMemberStatuses.allFor(profile.slackTeamId)
        }).map(_.flatten.filterNot(_.isBotOrApp))
      } yield {
        val groupedByIds = data.
          groupBy { ea => (ea.slackTeamId, ea.slackUserId) }.
          map { case(ids, statuses) => ids -> statuses.sortBy(_.firstObservedAt).reverse }
        val firstTimestamp = data.map(_.firstObservedAt).min
        var monthStart = startOfMonthFor(firstTimestamp)
        var counts = Seq[MonthData]()
        while (monthStart.isBefore(startOfMonthFor(OffsetDateTime.now))) {
          var activeCount = 0
          var inactiveCount = 0
          groupedByIds.foreach { case(ids, statuses) =>
            statuses.find(_.firstObservedAt.isBefore(monthStart)).foreach { status =>
              if (status.isDeleted) {
                inactiveCount = inactiveCount + 1
              } else {
                activeCount = activeCount + 1
              }
            }
          }
          counts = counts ++ Seq(MonthData(monthStart, activeCount, inactiveCount))
          monthStart = monthStart.plusMonths(1)
        }
        Ok(Json.prettyPrint(Json.toJson(counts)))
      }
    })
  }

}


