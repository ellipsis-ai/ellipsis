package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json

class StyleguideController @Inject() (
  val silhouette: Silhouette[EllipsisEnv],
  val configuration: Configuration,
  val assetsProvider: Provider[RemoteAssets]
) extends EllipsisController {

  def colors = silhouette.UserAwareAction { implicit request =>
    render {
      case Accepts.JavaScript() => {
        Ok(views.js.shared.webpackLoader(
          viewConfig(None), "ColorsConfig", "styleguideColors", Json.obj(
            "containerId" -> "colorContainer"
          )))
      }
      case Accepts.Html() => Ok(views.html.styleguide.colors(viewConfig(None)))
    }
  }
}
