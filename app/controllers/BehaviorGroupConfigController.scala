package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.Formatting._
import json.{ScheduledActionsConfig, UserData}
import models.ViewConfig
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.DefaultServices
import utils.FutureSequencer
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

  private case class TeamUserData(teamId: String, users: Seq[UserData])

  private implicit val teamUserDataFormat = Json.format[TeamUserData]

  def teamUserData(teamId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(teamId))
      result <- teamAccess.maybeTargetTeam.map { team =>
        for {
          users <- dataService.users.allFor(team)
          usersData <- FutureSequencer.sequence(users, (ea: User) =>
            dataService.users.userDataFor(ea, team)
          )
        } yield {
          Ok(Json.toJson(TeamUserData(teamId, usersData)))
        }
      }.getOrElse {
        Future.successful(NotFound("Team not found"))
      }
    } yield result
  }

  private case class SetContactInfo(contactId: Option[String])

  private implicit val setContactInfoFormat = Json.format[SetContactInfo]

  def setManagedContact(groupId: String) = silhouette.SecuredAction(parse.json).async { request =>
    request.body.validate[SetContactInfo].fold(
      errors => {
        Future.successful(BadRequest(Json.toJson(errors.map(ea => (ea._1.toString(), ea._2.map(_.message))))))
      },
      contactInfo => {
        for {
          maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
          maybeContact <- contactInfo.contactId.map { contactId =>
            dataService.users.find(contactId)
          }.getOrElse(Future.successful(None))
          result <- maybeGroup.map { group =>
            dataService.managedBehaviorGroups.ensureFor(group, maybeContact).map { _ =>
              Ok(Json.toJson(contactInfo))
            }
          }.getOrElse(Future.successful(NotFound(s"Skill not found: ${groupId}")))
        } yield result
      }
    )
  }
}
