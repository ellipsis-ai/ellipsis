package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ScheduledActionsController @Inject() (
                                             val messagesApi: MessagesApi,
                                             val configuration: Configuration,
                                             val silhouette: Silhouette[EllipsisEnv],
                                             val dataService: DataService
                                            ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      scheduledMessages <- teamAccess.maybeTargetTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
      scheduledBehaviors <- teamAccess.maybeTargetTeam.map { team =>
        dataService.scheduledBehaviors.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
      result <- teamAccess.maybeTargetTeam.map { team =>
        ScheduledActionsData.fromScheduleData(team.id, dataService, scheduledMessages, scheduledBehaviors).map { data =>
          val scheduledActionsJson = Json.toJson(data)
          Ok(views.html.scheduledactions.index(
            viewConfig(Some(teamAccess)),
            Json.prettyPrint(scheduledActionsJson)
          ))
        }
      }.getOrElse {
        Future.successful(NotFound("Team not accessible"))
      }
    } yield result
  }
}
