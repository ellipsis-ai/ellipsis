package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.Silhouette
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import play.api.mvc.{AnyContent, RequestHeader, Result}

trait ReAuthable extends EllipsisController {

  val silhouette: Silhouette[EllipsisEnv]

  protected def reAuthLinkFor(request: RequestHeader, maybeSlackTeamId: Option[String]) = {
    routes.SocialAuthController.authenticateSlack(
      Some(request.uri),
      maybeSlackTeamId,
      None
    )
  }

  protected def withAuthDiscarded(request: SecuredRequest[EllipsisEnv, AnyContent], result: Result)(implicit r: RequestHeader) = {
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  protected def reAuthFor(request: SecuredRequest[EllipsisEnv, AnyContent], maybeTeamId: Option[String])(implicit r: RequestHeader) = {
    withAuthDiscarded(request, Redirect(reAuthLinkFor(request, maybeTeamId)))
  }

  protected def notFoundWithLoginFor(
                                       request: SecuredRequest[EllipsisEnv, AnyContent],
                                       maybeTeamAccess: Option[UserTeamAccess],
                                       maybeHeading: Option[String] = None,
                                       maybeErrorMessage: Option[String] = None
                                     )(implicit r: RequestHeader) = {
    NotFound(
      views.html.error.notFound(
        viewConfig(maybeTeamAccess),
        maybeHeading,
        maybeErrorMessage,
        Some(reAuthLinkFor(request, None))
      ))
  }

}
