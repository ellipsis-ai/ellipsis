package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.admin.AdminAuth
import javax.inject.Inject
import json.{SkillManifestConfig, UsageReportConfig}
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

  def usage(
             maybeTeamId: Option[String]
           ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    withIsAdminCheck(() => {
      render.async {
        case Accepts.JavaScript() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            teamAccess.maybeTargetTeam.map { team =>
              Ok(views.js.shared.webpackLoader(
                viewConfig(Some(teamAccess)),
                "UsageReportConfig",
                "dashboardUsage",
                Json.toJson(UsageReportConfig.buildForDemoData(
                  "usageReportContainer",
                  CSRF.getToken(request).value,
                  teamAccess.isAdminAccess,
                  team.id
                ))
              ))
            }.getOrElse {
              NotFound("Team not found")
            }
          }
        }
        case Accepts.Html() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            val myViewConfig = viewConfig(Some(teamAccess))
            Ok(views.html.dashboard.usage(
              myViewConfig,
              maybeTeamId
            ))
          }
        }
      }
    })
  }

  def skillManifest(
                     maybeTeamId: Option[String]
                   ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    withIsAdminCheck(() => {
      render.async {
        case Accepts.JavaScript() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            teamAccess.maybeTargetTeam.map { team =>
              Ok(views.js.shared.webpackLoader(
                viewConfig(Some(teamAccess)),
                "SkillManifestConfig",
                "dashboardSkillManifest",
                Json.toJson(SkillManifestConfig(
                  "skillManifestContainer",
                  CSRF.getToken(request).value,
                  teamAccess.isAdminAccess,
                  team.id
                ))
              ))
            }.getOrElse {
              NotFound("Team not found")
            }
          }
        }
        case Accepts.Html() => {
          for {
            teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          } yield {
            val myViewConfig = viewConfig(Some(teamAccess))
            Ok(views.html.dashboard.skillManifest(
              myViewConfig,
              maybeTeamId
            ))
          }
        }
      }
    })
  }
}
