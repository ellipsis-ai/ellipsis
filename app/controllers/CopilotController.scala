package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.MessageListenerData
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
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

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      listeners <- dataService.messageListeners.allForUser(user)
      listenerData <- Future.sequence(listeners.map(ea => MessageListenerData.from(ea, teamAccess, dataService)))
    } yield {
      val listenerStrings = listenerData.map { ea =>
        s"Action ${ea.action.flatMap(_.name).get} is triggered for messages in ${ea.channel}"
      }.mkString("\n")
      Ok(listenerStrings)
    }
  }

}
