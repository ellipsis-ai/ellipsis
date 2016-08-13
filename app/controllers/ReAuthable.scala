package controllers

import com.mohiva.play.silhouette.api.{Silhouette, Environment}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.accounts.User
import play.api.mvc.{RequestHeader, Result, AnyContent}
import slick.dbio.DBIO

trait ReAuthable extends Silhouette[User, CookieAuthenticator] {

  val socialProviderRegistry: SocialProviderRegistry
  val env: Environment[User, CookieAuthenticator]

  protected def reAuthLinkFor(request: RequestHeader, maybeTeamId: Option[String]) = {
    routes.SocialAuthController.authenticateSlack(
      Some(request.uri),
      maybeTeamId,
      None
    )
  }

  protected def withAuthDiscarded(request: SecuredRequest[AnyContent], result: Result)(implicit r: RequestHeader) = {
    DBIO.from(env.authenticatorService.discard(request.authenticator, result))
  }

  protected def reAuthFor(request: SecuredRequest[AnyContent], maybeTeamId: Option[String])(implicit r: RequestHeader) = {
    withAuthDiscarded(request, Redirect(reAuthLinkFor(request, maybeTeamId)))
  }
}
