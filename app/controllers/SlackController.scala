package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.Models
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.utils.UriEncoding
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class SlackController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val configuration: Configuration,
                                  val models: Models,
                                  socialProviderRegistry: SocialProviderRegistry)
  extends EllipsisController {

  def add = silhouette.UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getString("silhouette.slack.scope")
      clientId <- configuration.getString("silhouette.slack.clientID")
    } yield {
        val redirectUrl = routes.SocialAuthController.installForSlack().absoluteURL(secure=true)
        Ok(views.html.addToSlack(scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
  }

  def signIn(maybeRedirectUrl: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    val eventualMaybeTeamAccess = request.identity.map { user =>
      user.teamAccessFor(None).map(Some(_))
    }.getOrElse(DBIO.successful(None))
    val action = eventualMaybeTeamAccess.map { maybeTeamAccess =>
      val maybeResult = for {
        scopes <- configuration.getString("silhouette.slack.signInScope")
        clientId <- configuration.getString("silhouette.slack.clientID")
      } yield {
          val redirectUrl = routes.SocialAuthController.authenticateSlack(maybeRedirectUrl).absoluteURL(secure=true)
          Ok(views.html.signInWithSlack(maybeTeamAccess, scopes, clientId, UriEncoding.encodePathSegment(redirectUrl, "utf-8")))
        }
      maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
    }

    models.run(action)
  }


}
