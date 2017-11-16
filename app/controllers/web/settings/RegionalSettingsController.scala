package controllers.web.settings

import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import json.Formatting._
import json.RegionalSettingsConfig
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json._
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.ExecutionContext

class RegionalSettingsController @Inject()(
                                            val silhouette: Silhouette[EllipsisEnv],
                                            val dataService: DataService,
                                            val configuration: Configuration,
                                            val assetsProvider: Provider[RemoteAssets],
                                            implicit val ec: ExecutionContext
                                          ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val maybeTz = team.maybeTimeZone
            val config = RegionalSettingsConfig(
              "regionalSettings",
              CSRF.getToken(request).map(_.value),
              teamAccess.isAdminAccess,
              team.id,
              maybeTz.map(_.toString),
              maybeTz.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
              maybeTz.map(tz => OffsetDateTime.now(tz).getOffset.getTotalSeconds)
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "settings/regionalsettings/index", Json.toJson(config)))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield teamAccess.maybeTargetTeam.map { team =>
          val dataRoute = routes.RegionalSettingsController.index(maybeTeamId)
          Ok(views.html.web.settings.regionalsettings.index(viewConfig(Some(teamAccess)), dataRoute))
        }.getOrElse {
          notFoundWithLoginFor(request, Some(teamAccess))
        }
      }
    }
  }

}
