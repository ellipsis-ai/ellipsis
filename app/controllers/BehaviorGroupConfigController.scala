package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.silhouette.EllipsisEnv
import play.api.mvc.{Action, AnyContent}
import services.DefaultServices

import scala.concurrent.ExecutionContext

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
          maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
          teamAccess <- dataService.users.teamAccessFor(user, maybeGroup.map(_.team.id))
          isAdmin <- dataService.users.isAdmin(user)
        } yield {
          (for {
            group <- maybeGroup
            team <- teamAccess.maybeTargetTeam
          } yield {
            Ok(s"You’re accessing schedules for skill ID ${group.id} on team ${team.name}")
          }).getOrElse {
            NotFound("Skill not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
          teamAccess <- dataService.users.teamAccessFor(user, maybeGroup.map(_.team.id))
          isAdmin <- dataService.users.isAdmin(user)
        } yield {
          (for {
            group <- maybeGroup
            team <- teamAccess.maybeTargetTeam
          } yield {
            Ok(s"You’re accessing schedules for skill ID ${group.id} on team ${team.name}")
          }).getOrElse {
            NotFound("Skill not found")
          }
        }
      }
    }
  }
}
