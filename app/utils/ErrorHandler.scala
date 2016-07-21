package utils

import javax.inject.Inject

import com.mohiva.play.silhouette.api.SecuredErrorHandler
import controllers.routes
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, OptionalSourceMapper, UsefulException}

import scala.concurrent.Future

/**
 * A secured error handler.
 */
class ErrorHandler @Inject() (
                               env: play.api.Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: javax.inject.Provider[Router],
                               messagesApi: MessagesApi
                               )
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with SecuredErrorHandler {

  override def onNotAuthenticated(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    // TODO: platform-agnostic
    Some(Future.successful(Redirect(routes.SlackController.signIn(Some(request.uri)))))
  }

  override def onNotAuthorized(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    Some(Future.successful(Ok("not authorized")))
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val r = request
    implicit val m = messagesApi.preferred(request)
    val maybeNonEmptyMessage = Option(message).filter(_.trim.nonEmpty)
    Future.successful(
      NotFound(
        views.html.notFound(
          None,
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
        views.html.serverError(
          Some(exception.title),
          Some(exception.description),
          Some(exception.id)
        )
      )
    )
  }

}
