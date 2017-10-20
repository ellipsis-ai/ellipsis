package controllers

import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
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
            val config = team.maybeTimeZone.map { tz =>
              val now = OffsetDateTime.now(tz)
              RegionalSettingsConfig(
                "regionalSettings",
                CSRF.getToken(request).map(_.value),
                team.id,
                Some(tz.toString),
                Some(tz.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                Some(now.getOffset.getTotalSeconds)
              )
            }.getOrElse {
              RegionalSettingsConfig(
                "regionalSettings",
                CSRF.getToken(request).map(_.value),
                team.id,
                None,
                None,
                None
              )
            }
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/regionalsettings/index", Json.toJson(config)))
          }.getOrElse{
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield teamAccess.maybeTargetTeam.map { team =>
          val dataRoute = routes.RegionalSettingsController.index(maybeTeamId)
          Ok(views.html.regionalsettings.index(viewConfig(Some(teamAccess)), dataRoute))
        }.getOrElse {
          notFoundWithLoginFor(request, Some(teamAccess))
        }
      }
    }
  }

}
