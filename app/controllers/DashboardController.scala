package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.admin.AdminAuth
import javax.inject.Inject
import json.DashboardConfig
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.DefaultServices
import views.html.helper.CSRF

import scala.concurrent.ExecutionContext

class DashboardController @Inject()(
                                     val configuration: Configuration,
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val services: DefaultServices,
                                     val assetsProvider: Provider[RemoteAssets],
                                     implicit val actorSystem: ActorSystem,
                                     implicit val ec: ExecutionContext
                                   ) extends ReAuthable with AdminAuth {
  val dataService = services.dataService

  def index(
             maybeTeamId: Option[String]
           ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    withIsAdminCheck(() => {
      render.async {
        case Accepts.JavaScript() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "DashboardConfig",
              "dashboard",
              Json.toJson(DashboardConfig.buildForDemoData(
                "dashboardContainer",
                CSRF.getToken(request).value
              ))
            ))
          }
        }
        case Accepts.Html() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            val myViewConfig = viewConfig(Some(teamAccess))
            Ok(views.html.dashboard.index(
              myViewConfig,
              maybeTeamId
            ))
          }
        }
      }
    })
  }
}
