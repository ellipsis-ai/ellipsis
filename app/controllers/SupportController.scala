package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.Formatting._
import json.SupportRequestConfig
import models.behaviors.builtins.FeedbackBehavior
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import play.filters.csrf.CSRF
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

class SupportController @Inject() (
                                    val configuration: Configuration,
                                    val services: DefaultServices,
                                    val silhouette: Silhouette[EllipsisEnv],
                                    val assetsProvider: Provider[RemoteAssets],
                                    implicit val actorSystem: ActorSystem,
                                    implicit val ec: ExecutionContext
                                  ) extends EllipsisController {

  val dataService: DataService = services.dataService

  def request: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    for {
      maybeTeamAccess <- maybeTeamAccessFor(request, dataService)
      maybeUserData <- maybeTeamAccess.map { teamAccess =>
        dataService.users.userDataFor(teamAccess.user, teamAccess.loggedInTeam).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      val viewConfigData = viewConfig(maybeTeamAccess)
      render {
        case Accepts.JavaScript() =>
          Ok(views.js.shared.webpackLoader(
            viewConfigData,
            "SupportRequestConfig",
            "supportRequest",
            Json.toJson(SupportRequestConfig(
              containerId = "supportRequest",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = maybeTeamAccess.map(_.loggedInTeam.id),
              user = maybeUserData
            ))
          ))
        case Accepts.Html() =>
          Ok(views.html.support.request(viewConfigData))
      }
    }
  }

  case class SendRequestInfo(name: String, emailAddress: String, message: String)

  implicit val sendRequestInfoFormat = Json.format[SendRequestInfo]

  def sendRequest: Action[JsValue] = silhouette.UserAwareAction(parse.json).async { implicit request =>
    request.body.validate[SendRequestInfo].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors))))
      },
      info => {
        val maybeUser = request.identity
        for {
          maybeTeam <- maybeUser.map { user =>
            dataService.teams.find(user.teamId)
          }.getOrElse(Future.successful(None))
          didSend <- FeedbackBehavior
            .supportRequest(maybeUser, maybeTeam, services, info.name, info.emailAddress, info.message)
        } yield {
          if (didSend) {
            Ok(Json.toJson(info))
          } else {
            InternalServerError(Json.obj("errors" -> Seq("The support request did not send. Please try again.")))
          }
        }
      }
    )
  }
}
