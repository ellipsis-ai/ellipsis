package controllers

import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.{InvocationLogEntryData, MessageListenerData}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.filters.csrf.CSRF
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

class CopilotController @Inject()(
                                    val silhouette: Silhouette[EllipsisEnv],
                                    val configuration: Configuration,
                                    val services: DefaultServices,
                                    val ws: WSClient,
                                    val assetsProvider: Provider[RemoteAssets],
                                    implicit val actorSystem: ActorSystem,
                                    implicit val ec: ExecutionContext
                                  ) extends ReAuthable {

  val dataService: DataService = services.dataService

  case class CopilotConfig(containerId: String, csrfToken: Option[String])

  implicit lazy val copilotConfigFormat = Json.format[CopilotConfig]

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          Ok(views.js.shared.webpackLoader(
            viewConfig(None), "CopilotConfig", "copilot", Json.toJson(CopilotConfig("copilot", CSRF.getToken(request).map(_.value)))
          ))
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            val dataRoute = routes.CopilotController.index(maybeTeamId)
            Ok(views.html.copilot.index(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  case class ResultsData(results: Seq[InvocationLogEntryData])

  implicit lazy val resultsDataFormat = Json.format[ResultsData]

  def resultsSince(when: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val since = try {
      OffsetDateTime.parse(when)
    } catch {
      case _: DateTimeParseException => OffsetDateTime.now.minusDays(1)
    }
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      listeners <- dataService.messageListeners.allForUser(user)
      listenerData <- Future.sequence(listeners.map(ea => MessageListenerData.from(ea, teamAccess, dataService)))
      logEntries <- Future.sequence(listeners.map { ea =>
        dataService.invocationLogEntries.allForMessageListener(ea, since)
      }).map(_.flatten)
    } yield {
      val resultsData = ResultsData(logEntries.map { ea =>
        InvocationLogEntryData(
          ea.behaviorVersion.behavior.id,
          ea.resultType,
          ea.messageText,
          ea.resultText,
          ea.context,
          ea.maybeChannel,
          ea.maybeUserIdForContext,
          ea.maybeOriginalEventType.map(_.toString),
          ea.runtimeInMilliseconds,
          ea.createdAt
        )
      }.sortBy(_.createdAt))
      Ok(Json.toJson(resultsData))
    }
  }

}
