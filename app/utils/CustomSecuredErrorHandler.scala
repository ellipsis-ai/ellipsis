package utils

import javax.inject.Inject

import com.google.inject.Singleton
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
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
}
