package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models.behaviors.builtins.FeedbackBehavior
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

class FeedbackController @Inject()(
                                    val silhouette: Silhouette[EllipsisEnv],
                                    val configuration: Configuration,
                                    val services: DefaultServices,
                                    val ws: WSClient,
                                    val assetsProvider: Provider[RemoteAssets],
                                    implicit val actorSystem: ActorSystem,
                                    implicit val ec: ExecutionContext
                                 ) extends ReAuthable {

  val dataService: DataService = services.dataService

  case class FeedbackInfo(message: String)

  private val feedbackForm = Form(
    mapping(
      "message" -> nonEmptyText
    )(FeedbackInfo.apply)(FeedbackInfo.unapply)
  )

  def send = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    feedbackForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, None)
          maybeWasSent <- teamAccess.maybeTargetTeam.map { team =>
            FeedbackBehavior.feedbackFor(user, team, services, "feedback", info.message).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeWasSent.map { wasSent =>
            Ok(Json.toJson(wasSent))
          }.getOrElse {
            NotFound("Page not found")
          }
        }
      }
    )
  }

}
