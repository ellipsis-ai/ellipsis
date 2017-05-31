package utils

import javax.inject.Inject

import models.ViewConfig
import play.api.http.{ContentTypes, DefaultHttpErrorHandler}
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{Rendering, RequestExtractors, RequestHeader, Result}
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
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
    with ContentTypes
    with RequestExtractors
    with Rendering {

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

  override def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val r = request
    implicit val messages = messagesApi.preferred(request)
    render.async {
      case Accepts.JavaScript() => {
        Future.successful(Ok(views.js.error.jsError(s"${exception.title}: ${exception.description}")))
      }
      case Accepts.Html() => super.onDevServerError(request, exception)
    }
// Use the code below to test production server errors
//    Future.successful(
//      InternalServerError(
//        views.html.error.serverError(
//          ViewConfig(config, None), Some(exception.id)
//        )
//      )
//    )
  }
}
