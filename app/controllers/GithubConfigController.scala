package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class GithubConfigController @Inject() (
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val configuration: Configuration,
                                     val dataService: DataService,
                                     val assetsProvider: Provider[RemoteAssets],
                                     implicit val ec: ExecutionContext
                                   ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          maybeGithubLinkedAccount <- if (teamAccess.isAdminAccess) {
            Future.successful(None)
          } else {
            dataService.linkedAccounts.maybeForGithubFor(user)
          }
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = GithubConfigConfig(
              containerId = "githubConfig",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamAccess.isAdminAccess,
              team.id,
              linkedAccount = maybeGithubLinkedAccount.map(LinkedAccountData.from)
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/github/index", Json.toJson(config)))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            val dataRoute = routes.GithubConfigController.index(maybeTeamId)
            Ok(views.html.githubconfig.index(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  def reset = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      isDeleted <- dataService.linkedAccounts.deleteGithubFor(user)
    } yield {
      println(s"deleted: $isDeleted")
      Redirect(routes.GithubConfigController.index())
    }
  }

}
