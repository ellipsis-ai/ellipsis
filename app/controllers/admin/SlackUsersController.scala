package controllers.admin


import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import javax.inject.Inject
import models.silhouette.EllipsisEnv
import play.api.Configuration
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

}


