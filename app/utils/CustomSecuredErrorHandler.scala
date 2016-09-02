package utils

import javax.inject.Inject

import com.google.inject.Singleton
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import play.api.UsefulException
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

@Singleton
class CustomSecuredErrorHandler @Inject() (
                                            val messagesApi: MessagesApi
                                          ) extends SecuredErrorHandler with I18nSupport  {

  def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = {
    val path = request.uri
    val maybeRedirect = if (path == "/") {
      None
    } else {
      Some(path)
    }
    // TODO: platform-agnostic
    Future.successful(Results.Redirect(controllers.routes.SlackController.signIn(maybeRedirect)))
  }

  def onNotAuthorized(implicit request: RequestHeader): Future[Result] = {
    Future.successful(Results.Ok("not authorized"))
  }

  def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val r = request
    val maybeNonEmptyMessage = Option(message).filter(_.trim.nonEmpty)
    Future.successful(
      Results.NotFound(
        views.html.notFound(
          None,
          None,
          maybeNonEmptyMessage
        )
      )
    )
  }

  def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val r = request
    Future.successful(
      Results.InternalServerError(
        views.html.serverError(
          Some(exception.id)
        )
      )
    )
  }

  /* Uncomment to test the custom error handler locally
  def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val r = request
    implicit val messages = messagesApi.preferred(request)
    Future.successful(
      InternalServerError(
        views.html.serverError(
          Some(exception.id)
        )
      )
    )
  }
  */
}
