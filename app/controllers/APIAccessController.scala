package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.IDs
import models.accounts.linkedoauth2token.{LinkedOAuth2Token, LinkedOAuth2TokenInfo}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Result}
import services.{CacheService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIAccessController @Inject() (
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val configuration: Configuration,
                                      val dataService: DataService,
                                      val ws: WSClient,
                                      val cacheService: CacheService,
                                      val eventHandler: EventHandler,
                                      val botResultService: BotResultService,
                                      val assetsProvider: Provider[RemoteAssets],
                                      implicit val actorSystem: ActorSystem
                                    )
  extends ReAuthable {

  private def getToken(code: String, application: OAuth2Application, user: User, redirectUrl: String): Future[Option[LinkedOAuth2Token]] = {
    application.accessTokenResponseFor(code, redirectUrl, ws).flatMap { response =>
      LinkedOAuth2TokenInfo.maybeFrom(response.json).map { info =>
        val token = LinkedOAuth2Token(info.accessToken, info.maybeTokenType, info.maybeExpirationTime, info.maybeRefreshToken, info.maybeScopeGranted, user.id, application)
        dataService.linkedOAuth2Tokens.save(token).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  private def maybeResultWithMagicLinkFor(
                                      invocationId: String
                                    )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[Future[Result]] = {
    cacheService.getEvent(invocationId).map { event =>
      eventHandler.handle(event, None).map { results =>
        results.map(ea => botResultService.sendIn(ea, None))
        Redirect(routes.APIAccessController.authenticated(s"There should now be a response in ${event.name}."))
      }
    }
  }

  private def resultWithToken(
                               maybeRedirectAfterAuth: Option[String]
                             )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    request.session.get("invocation-id").flatMap { invocationId =>
      maybeResultWithMagicLinkFor(invocationId)
    }.getOrElse {
      val redirect = maybeRedirectAfterAuth.getOrElse {
        routes.APIAccessController.authenticated(s"You are now authenticated and can try again.").toString
      }
      Future.successful(Redirect(redirect))
    }
  }

  private def resultForStep1(
                            application: OAuth2Application,
                            maybeRedirectAfterAuth: Option[String],
                            maybeInvocationId: Option[String]
                            )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    val state = IDs.next
    val redirectParam = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, maybeRedirectAfterAuth).absoluteURL(secure = true)
    val maybeRedirect = application.maybeAuthorizationRequestFor(state, redirectParam, ws).map { r =>
      r.uri.toString
    }
    val result = maybeRedirect.map { redirect =>
      val sessionState = Seq(Some("oauth-state" -> state), maybeInvocationId.map(id => "invocation-id" -> id)).flatten
      Redirect(redirect).withSession(sessionState: _*)
    }.getOrElse(BadRequest("Doesn't use authorization code"))
    Future.successful(result)
  }

  private def resultForStep2(
                            state: String,
                            oauthState: String,
                            code: String,
                            user: User,
                            application: OAuth2Application,
                            maybeInvocationId: Option[String],
                            maybeRedirectAfterAuth: Option[String]
                            )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    if (state == oauthState) {
      val redirect = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, maybeInvocationId, maybeRedirectAfterAuth).absoluteURL(secure = true)
      getToken(code, application, user, redirect).flatMap { maybeLinkedToken =>
        maybeLinkedToken.
          map(_ => resultWithToken(maybeRedirectAfterAuth)).
          getOrElse(Future.successful(BadRequest("boom")))
      }
    } else {
      Future.successful(BadRequest("Invalid state"))
    }
  }

  private def maybeResultForStep2(
                                   codeOpt: Option[String],
                                   stateOpt: Option[String],
                                   user: User,
                                   application: OAuth2Application,
                                   maybeInvocationId: Option[String],
                                   maybeRedirectAfterAuth: Option[String]
                                 )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[Future[Result]] = {
    for {
      code <- codeOpt
      state <- stateOpt
      oauthState <- request.session.get("oauth-state")
    } yield {
      resultForStep2(state, oauthState, code, user, application, maybeInvocationId, maybeRedirectAfterAuth)
    }
  }

  private def noApplicationResult(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    Future.successful(
      NotFound(views.html.error.notFound(viewConfig(None), Some("Can't find OAuth2 application"), None, None))
    )
  }

  def linkCustomOAuth2Service(
                               applicationId: String,
                               codeOpt: Option[String],
                               stateOpt: Option[String],
                               maybeInvocationId: Option[String],
                               maybeRedirectAfterAuth: Option[String]
                               ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeApplication <- dataService.oauth2Applications.find(applicationId)
      result <- maybeApplication.map { application =>
        dataService.teams.find(application.teamId, user).map(_.isDefined).flatMap { isLoggedInToCorrectTeam =>
          if (application.isShared || isLoggedInToCorrectTeam) {
            maybeResultForStep2(codeOpt, stateOpt, user, application, maybeInvocationId, maybeRedirectAfterAuth).getOrElse {
              resultForStep1(application, maybeRedirectAfterAuth, maybeInvocationId)
            }
          } else {
            reAuthFor(request, maybeApplication.map(_.teamId))
          }
        }
      }.getOrElse(noApplicationResult)
    } yield result
  }

  def authenticated(message: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    dataService.teams.find(user.teamId).map { maybeTeam =>
      Ok(views.html.apiaccess.authenticated(viewConfig(None), maybeTeam, message))
    }
  }


}
