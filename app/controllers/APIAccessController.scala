package controllers

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject.Inject
import models.accounts.OAuth2State
import models.accounts.linkedoauth1token.LinkedOAuth1Token
import models.accounts.linkedoauth2token.{LinkedOAuth2Token, LinkedOAuth2TokenInfo}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.oauth._
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, RequestHeader, Result}
import services.DataService
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}

class APIAccessController @Inject() (
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val configuration: Configuration,
                                      val dataService: DataService,
                                      val ws: WSClient,
                                      val cacheService: CacheService,
                                      val eventHandler: EventHandler,
                                      val botResultService: BotResultService,
                                      val assetsProvider: Provider[RemoteAssets],
                                      implicit val actorSystem: ActorSystem,
                                      implicit val ec: ExecutionContext
                                    ) extends ReAuthable {

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
                                    )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Option[Result]] = {
    for {
      maybeEvent <- cacheService.getEvent(invocationId)
      maybeResult <- maybeEvent.map { event =>
        eventHandler.handle(event, None).map { results =>
          results.map(ea => botResultService.sendIn(ea, None))
          Some(Redirect(routes.APIAccessController.authenticated(s"There should now be a response in ${event.eventContext.name}.")))
        }
      }.getOrElse(Future.successful(None))
    } yield maybeResult
  }

  private def fallbackResultFor(maybeRedirectAfterAuth: Option[String]): Result = {
    val redirect = maybeRedirectAfterAuth.getOrElse {
      routes.APIAccessController.authenticated(s"You are now authenticated and can try again.").toString
    }
    Redirect(redirect)
  }

  private def resultWithToken(
                                maybeInvocationId: Option[String],
                                maybeRedirectAfterAuth: Option[String]
                             )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    maybeInvocationId.map { invocationId =>
      maybeResultWithMagicLinkFor(invocationId).map { maybeResult =>
        maybeResult.getOrElse(fallbackResultFor(maybeRedirectAfterAuth))
      }
    }.getOrElse(Future.successful(fallbackResultFor(maybeRedirectAfterAuth)))
  }

  private def resultForStep1(
                            application: OAuth2Application,
                            state: OAuth2State
                            )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    val redirectParam = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None).absoluteURL(secure = true)
    val maybeRedirect = application.maybeAuthorizationRequestFor(state, redirectParam, ws).map { r =>
      r.uri.toString
    }
    val result = maybeRedirect.map { redirect =>
      val sessionState = Seq("oauth-state" -> state.id)
      Redirect(redirect).withSession(sessionState: _*)
    }.getOrElse(BadRequest("Doesn't use authorization code"))
    Future.successful(result)
  }

  private def resultForStep2(
                            state: OAuth2State,
                            oauthState: String,
                            code: String,
                            user: User,
                            application: OAuth2Application
                            )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    if (state.id == oauthState) {
      val redirect = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None).absoluteURL(secure = true)
      getToken(code, application, user, redirect).flatMap { maybeLinkedToken =>
        maybeLinkedToken.
          map(_ => resultWithToken(state.maybeInvocationId, state.maybeRedirectAfterAuth)).
          getOrElse(Future.successful(BadRequest("boom")))
      }
    } else {
      Future.successful(BadRequest("Invalid state"))
    }
  }

  private def maybeResultForStep2(
                                   codeOpt: Option[String],
                                   state: OAuth2State,
                                   user: User,
                                   application: OAuth2Application
                                 )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[Future[Result]] = {
    for {
      code <- codeOpt
      oauthState <- request.session.get("oauth-state")
    } yield {
      resultForStep2(state, oauthState, code, user, application)
    }
  }

  private def noApplicationResult(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    Future.successful(
      NotFound(views.html.error.notFound(viewConfig(None), Some("Can't find OAuth configuration"), None, None))
    )
  }

  def linkCustomOAuth2Service(
                               applicationId: String,
                               codeOpt: Option[String],
                               stateOpt: Option[String]
                               ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val state = OAuth2State.ensureFor(stateOpt)
    for {
      maybeApplication <- dataService.oauth2Applications.find(applicationId)
      result <- maybeApplication.map { application =>
        dataService.teams.find(application.teamId, user).map(_.isDefined).flatMap { isLoggedInToCorrectTeam =>
          if (application.isShared || isLoggedInToCorrectTeam) {
            maybeResultForStep2(codeOpt, state, user, application).getOrElse {
              resultForStep1(application, state)
            }
          } else {
            reAuthFor(request, maybeApplication.map(_.teamId))
          }
        }
      }.getOrElse(noApplicationResult)
    } yield result
  }

  private def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }

  def linkCustomOAuth1Service(
                               applicationId: String,
                               maybeInvocationId: Option[String],
                               maybeRedirectAfterAuth: Option[String]
                             ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeApplication <- dataService.oauth1Applications.find(applicationId)
      result <- maybeApplication.map { application =>
        dataService.teams.find(application.teamId, user).map(_.isDefined).flatMap { isLoggedInToCorrectTeam =>
          if (application.isShared || isLoggedInToCorrectTeam) {
            val api = application.api
            val key = ConsumerKey(application.consumerKey, application.consumerSecret)
            val scopePart = application.maybeScope.map(scope => s"&scope=$scope").getOrElse("")
            val authorizationUrlToUse = s"${api.authorizationUrl}?name=Ellipsis${scopePart}"
            val serviceInfo = ServiceInfo(
              api.requestTokenUrl,
              api.accessTokenUrl,
              authorizationUrlToUse,
              key
            )
            val oauth = OAuth(serviceInfo, use10a=true)
            request.getQueryString("oauth_verifier").map { verifier =>
              val tokenPair = sessionTokenPair(request).get
              oauth.retrieveAccessToken(tokenPair, verifier) match {
                case Right(t) => {
                  // We received the authorized tokens in the OAuth object - store it before we proceed
                  val token = LinkedOAuth1Token(t.token, t.secret, user.id, application)
                  dataService.linkedOAuth1Tokens.save(token).flatMap { _ =>
                    resultWithToken(maybeInvocationId, maybeRedirectAfterAuth).map { r =>
                      r.withSession("token" -> t.token, "secret" -> t.secret)
                    }
                  }
                }
                case Left(e) => throw e
              }
            }.getOrElse {
              val callbackUrl = routes.APIAccessController.linkCustomOAuth1Service(application.id, maybeInvocationId, maybeRedirectAfterAuth).absoluteURL(secure = true)
              oauth.retrieveRequestToken(callbackUrl) match {
                case Right(t) => {
                  // We received the unauthorized tokens in the OAuth object - store it before we proceed
                  Future.successful(Redirect(oauth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret))
                }
                case Left(e) => throw e
              }
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
