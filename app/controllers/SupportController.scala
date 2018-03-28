package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.SupportRequestConfig
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.filters.csrf.CSRF
import services.DataService
import json.Formatting._

import scala.concurrent.{ExecutionContext, Future}

class SupportController @Inject() (
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    val silhouette: Silhouette[EllipsisEnv],
                                    val assetsProvider: Provider[RemoteAssets],
                                    implicit val actorSystem: ActorSystem,
                                    implicit val ec: ExecutionContext
                                  ) extends EllipsisController {

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

}
