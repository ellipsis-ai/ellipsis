package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json

class StyleguideController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[EllipsisEnv],
  val configuration: Configuration
) extends EllipsisController {

  def colors = silhouette.UserAwareAction { implicit request =>
    render {
      case Accepts.JavaScript() => Ok(views.js.shared.pageConfig("config/styleguide/colors", Json.obj(
        "containerId" -> "colorContainer"
      )))
      case Accepts.Html() => Ok(views.html.styleguide.colors(viewConfig(None)))
    }
  }
}
