package utils

import javax.inject.Inject

import com.google.inject.Singleton
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import play.api.http.ContentTypes
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class CustomSecuredErrorHandler @Inject() (
                                            val messagesApi: MessagesApi
                                          ) extends SecuredErrorHandler
  with I18nSupport
  with ContentTypes
  with RequestExtractors
  with Rendering {

  def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = {
    val path = request.uri
    val isIndex = path == "/"
    val maybeRedirect = if (isIndex) {
      None
    } else {
      Some(path)
    }
    render.async {
      case Accepts.JavaScript() => {
        if (isIndex) {
          redirectToSignIn(maybeRedirect)
        } else {
          Future.successful(
            Results.Unauthorized("Authorization required\n").withHeaders(
              "WWW-Authenticate" -> s"""Bearer realm="${request.host}""""
            )
          )
        }
      }
      case Accepts.Html() => redirectToSignIn(maybeRedirect)
    }

  }

  def redirectToSignIn(maybeRedirect: Option[String])(implicit request: RequestHeader): Future[Result] = {
    Future.successful(Results.Redirect(controllers.routes.SocialAuthController.signIn(maybeRedirect)))
  }

  def onNotAuthorized(implicit request: RequestHeader): Future[Result] = {
    Future.successful(Results.Ok("not authorized"))
  }
}
