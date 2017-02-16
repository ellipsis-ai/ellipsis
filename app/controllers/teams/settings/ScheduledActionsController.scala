package controllers.teams.settings

import controllers.ReAuthable
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import json._
import json.Formatting._
import json.{EnvironmentVariableData, EnvironmentVariablesData}
import play.api.libs.json.Json
import play.api.Configuration
import services.DataService
import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.i18n.MessagesApi


class ScheduledActionsController @Inject() (
                                             val messagesApi: MessagesApi,
                                             val configuration: Configuration,
                                             val silhouette: Silhouette[EllipsisEnv],
                                             val dataService: DataService
                                            ) extends ReAuthable {

  def index(teamId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(teamId))
      scheduledMessages <- teamAccess.maybeTargetTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        val scheduledActionsJson = Json.toJson(ScheduledActionsData(team.id, scheduledMessages.map(sa =>ScheduledActionData(sa.text))))
        Ok(views.html.teams.settings.scheduled_actions.index(
          viewConfig(Some(teamAccess)),
          scheduledMessages,
          scheduledActionsJson.toString())
        )
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }
}
