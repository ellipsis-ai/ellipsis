package utils

import javax.inject.Inject

import models.ViewConfig
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, OptionalSourceMapper, UsefulException}

import scala.concurrent.Future

class ErrorHandler @Inject() (
                               env: play.api.Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: javax.inject.Provider[Router],
                               messagesApi: MessagesApi
                             )
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val r = request
    implicit val m = messagesApi.preferred(request)
    val maybeNonEmptyMessage = Option(message).filter(_.trim.nonEmpty)
    Future.successful(
      NotFound(
        views.html.error.notFound(
          ViewConfig(config, None),
          None,
          maybeNonEmptyMessage
        )
      )
    )
  }

  override def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val r = request
    implicit val messages = messagesApi.preferred(request)
    Future.successful(
      InternalServerError(
        views.html.error.serverError(
          ViewConfig(config, None), Some(exception.id)
        )
      )
    )
  }

  /* Uncomment to test the custom error handler locally
  override def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val r = request
    implicit val messages = messagesApi.preferred(request)
    Future.successful(
      InternalServerError(
        views.html.error.serverError(
          Some(exception.id)
        )
      )
    )
  }
  */
}
