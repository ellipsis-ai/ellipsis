package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject.Inject
import json.Formatting._
import json.{SkillManifestConfig, UsageReportConfig}
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import models.team.Team
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
  private def withValidTeamCheck(maybeTargetTeam: Option[String], fn: (UserTeamAccess, Team) => Future[Result])
                              (implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTargetTeam)
      result <- teamAccess.maybeTargetTeam.filter(_.id == PLENTY_TEAM_ID || teamAccess.isAdminUser).map { team =>
        fn(teamAccess, team)
      }.getOrElse {
        Future.successful(NotFound(views.html.error.notFound(viewConfig(None), None, None)))
      }
    } yield result
  }

  def usage(
             maybeTeamId: Option[String]
           ): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    withValidTeamCheck(maybeTeamId, (teamAccess, team) => {
      render.async {
        case Accepts.JavaScript() => Future.successful {
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
    withValidTeamCheck(maybeTeamId, (teamAccess, team) => {
      render.async {
        case Accepts.JavaScript() => {
          for {
            config <- SkillManifestConfig.buildFor(
              "skillManifestContainer",
              CSRF.getToken(request).value,
              teamAccess.isAdminAccess,
              team,
              dataService
            )
          } yield {
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "SkillManifestConfig",
              "dashboardSkillManifest",
              Json.toJson(config)
            ))
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
