package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.Models
import models.accounts.User
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.utils.UriEncoding

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
                                        socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def index = SecuredAction { implicit request => Ok("yay!") }

  def addToSlack = UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getString("silhouette.slack.scope")
      clientId <- configuration.getString("silhouette.slack.clientID")
      redirectUrl <- configuration.getString("silhouette.slack.redirectURL").map(UriEncoding.encodePathSegment(_, "utf-8"))
    } yield {
        Ok(views.html.addToSlack(request.identity, scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index))
  }

  def editBehavior = SecuredAction { implicit request =>
    Ok(views.html.edit())
  }

}
