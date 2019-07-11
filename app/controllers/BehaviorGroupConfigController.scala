package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.Formatting._
import json.ScheduledActionsConfig
import models.ViewConfig
import models.silhouette.EllipsisEnv
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.DefaultServices
import views.html.helper.CSRF

import scala.concurrent.{ExecutionContext, Future}

class BehaviorGroupConfigController @Inject()(
                                        val silhouette: Silhouette[EllipsisEnv],
                                        val services: DefaultServices,
                                        val assetsProvider: Provider[RemoteAssets],
                                        implicit val actorSystem: ActorSystem,
                                        implicit val ec: ExecutionContext
                                      ) extends ReAuthable {
  private val dataService = services.dataService

  def schedules(groupId: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          maybeGroup <- dataService.behaviorGroups.find(groupId, user)
          teamAccess <- dataService.users.teamAccessFor(user, maybeGroup.map(_.team.id))
          maybeConfig <- maybeGroup.map { group =>
            ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, group, CSRF.getToken(request).value)
          }.getOrElse(Future.successful(None))
        } yield {
          maybeConfig.map { config =>
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              configName = "BehaviorGroupSchedulingConfig",
              moduleToLoad = "behaviorGroupScheduling",
              Json.toJson(config)
            ))
          }.getOrElse {
            NotFound("Skill not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          maybeGroup <- dataService.behaviorGroups.find(groupId, user)
          teamAccess <- dataService.users.teamAccessFor(user, maybeGroup.map(_.team.id))
          maybeGroupVersion <- maybeGroup.map {
            group => dataService.behaviorGroupVersions.maybeCurrentFor(group)
          }.getOrElse(Future.successful(None))
        } yield {
          (for {
            group <- maybeGroup
            groupVersion <- maybeGroupVersion
          } yield {
            Ok(views.html.behaviorgroupconfig.scheduling(
              viewConfig(Some(teamAccess)),
              group.id,
              groupVersion.name,
              teamAccess.maybeAdminAccessToTeamId
            ))
          }).getOrElse {
            NotFound("Skill not found")
          }
        }
      }
    }
  }
}
