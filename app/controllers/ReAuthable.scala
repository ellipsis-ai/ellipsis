package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.mvc.{AnyContent, RequestHeader, Result}
import slick.dbio.DBIO

trait ReAuthable extends EllipsisController {

  val silhouette: Silhouette[EllipsisEnv]

  protected def reAuthLinkFor(request: RequestHeader, maybeTeamId: Option[String]) = {
    routes.SocialAuthController.authenticateSlack(
      Some(request.uri),
      maybeTeamId,
      None
    )
  }

  protected def withAuthDiscarded(request: SecuredRequest[EllipsisEnv, AnyContent], result: Result)(implicit r: RequestHeader) = {
    DBIO.from(silhouette.env.authenticatorService.discard(request.authenticator, result))
  }

  protected def reAuthFor(request: SecuredRequest[EllipsisEnv, AnyContent], maybeTeamId: Option[String])(implicit r: RequestHeader) = {
    withAuthDiscarded(request, Redirect(reAuthLinkFor(request, maybeTeamId)))
  }
}
