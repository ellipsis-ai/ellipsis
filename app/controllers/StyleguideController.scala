package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi

import scala.concurrent.Future

class StyleguideController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[EllipsisEnv],
  val configuration: Configuration
) extends EllipsisController {

  def colors = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.styleguide.colors(viewConfig(None))))
  }
}
