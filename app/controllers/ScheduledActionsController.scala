package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
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
                                             val dataService: DataService,
                                             implicit val actorSystem: ActorSystem
                                            ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          scheduledMessages <- teamAccess.maybeTargetTeam.map { team =>
            dataService.scheduledMessages.allForTeam(team)
          }.getOrElse(Future.successful(Seq()))
          scheduledBehaviors <- teamAccess.maybeTargetTeam.map { team =>
            dataService.scheduledBehaviors.allForTeam(team)
          }.getOrElse(Future.successful(Seq()))
          maybeBotProfile <- teamAccess.maybeTargetTeam.map { team =>
            dataService.slackBotProfiles.allFor(team).map(_.headOption)
          }.getOrElse(Future.successful(None))
          maybeChannels <- maybeBotProfile.map { botProfile =>
            Future.successful(Some(dataService.slackBotProfiles.channelsFor(botProfile)))
          }.getOrElse(Future.successful(None))
          maybeChannelInfo <- maybeChannels.map { channels =>
            channels.listInfos.map(Some(_))
          }.getOrElse(Future.successful(None))
          result <- teamAccess.maybeTargetTeam.map { team =>
            ScheduledActionsData.fromScheduleData(team.id, dataService, maybeChannelInfo, scheduledMessages, scheduledBehaviors).map { data =>
              val scheduledActionsJson = Json.toJson(data)
              Ok(views.js.shared.pageConfig("config/scheduling/index", scheduledActionsJson))
            }
          }.getOrElse {
            Future.successful(NotFound("Team not found"))
          }
        } yield result
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            Ok(views.html.scheduledactions.index(viewConfig(Some(teamAccess)), maybeTeamId))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }
}
