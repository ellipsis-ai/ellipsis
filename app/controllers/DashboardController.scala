package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.admin.AdminAuth
import javax.inject.Inject
import json.{SkillManifestConfig, UsageReportConfig}
import json.Formatting._
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import services.DefaultServices
import views.html.helper.CSRF

import scala.concurrent.{ExecutionContext, Future}

class DashboardController @Inject()(
                                     val configuration: Configuration,
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val services: DefaultServices,
                                     val assetsProvider: Provider[RemoteAssets],
                                     implicit val actorSystem: ActorSystem,
                                     implicit val ec: ExecutionContext
                                   ) extends ReAuthable {
  val dataService = services.dataService
  val PLENTY_TEAM_ID = "SZ4Mq9D_ROSPLVoxoinrhQ"

  // Hard-coded to allow admins and Plenty for now
  private def withValidTeamCheck(maybeTargetTeam: Option[String], fn: (UserTeamAccess) => Future[Result])
                              (implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTargetTeam)
      result <- if (teamAccess.isAdminUser || teamAccess.maybeTargetTeam.exists(_.id == PLENTY_TEAM_ID)) {
        fn(teamAccess)
      } else {
        Future.successful(NotFound(views.html.error.notFound(viewConfig(None), None, None)))
      }
    } yield result
  }

  def usage(
             maybeTeamId: Option[String]
           ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    withValidTeamCheck(maybeTeamId, (teamAccess) => {
      render.async {
        case Accepts.JavaScript() => Future.successful {
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
        case Accepts.Html() => Future.successful {
          val myViewConfig = viewConfig(Some(teamAccess))
          Ok(views.html.dashboard.usage(
            myViewConfig,
            maybeTeamId
          ))
        }
      }
    })
  }

  def skillManifest(
                     maybeTeamId: Option[String]
                   ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    withValidTeamCheck(maybeTeamId, (teamAccess) => {
      render.async {
        case Accepts.JavaScript() => Future.successful {
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
        case Accepts.Html() => Future.successful {
          val myViewConfig = viewConfig(Some(teamAccess))
          Ok(views.html.dashboard.skillManifest(
            myViewConfig,
            maybeTeamId
          ))
        }
      }
    })
  }
}
