package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.accounts.user.User
import models.IDs
import models.accounts.linkedoauth2token.LinkedOAuth2Token
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.events.{EventHandler, MessageEvent}
import models.silhouette.EllipsisEnv
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.http.{HeaderNames, MimeTypes}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Results
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIAccessController @Inject() (
                                      val messagesApi: MessagesApi,
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val configuration: Configuration,
                                      val dataService: DataService,
                                      val ws: WSClient,
                                      val cache: CacheApi,
                                      val eventHandler: EventHandler
                                    )
  extends ReAuthable {

  private def getToken(code: String, application: OAuth2Application, user: User, redirectUrl: String): Future[Option[LinkedOAuth2Token]] = {
    val tokenResponse =
      application.accessTokenRequestFor(code, redirectUrl, ws).
        withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
        post(Results.EmptyContent())

    tokenResponse.flatMap { response =>
      val json = response.json
      (json \ "access_token").asOpt[String].map { accessToken =>
        val maybeTokenType = (json \ "token_type").asOpt[String]
        val maybeScopeGranted = (json \ "scope").asOpt[String]
        val maybeExpirationTime = (json \ "expires_in").asOpt[Int].map { seconds =>
          DateTime.now.plusSeconds(seconds)
        }
        val maybeRefreshToken = (json \ "refresh_token").asOpt[String]
        val token = LinkedOAuth2Token(accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, maybeScopeGranted, user.id, application)
        dataService.linkedOAuth2Tokens.save(token).map(Some(_))
      }.getOrElse(Future.successful(None))

    }
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
          if (isLoggedInToCorrectTeam) {
            (for {
              code <- codeOpt
              state <- stateOpt
              oauthState <- request.session.get("oauth-state")
            } yield {
              if (state == oauthState) {
                val redirect = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, maybeInvocationId, maybeRedirectAfterAuth).absoluteURL(secure = true)
                getToken(code, application, user, redirect).flatMap { maybeLinkedToken =>
                  maybeLinkedToken.
                    map { _ =>
                      request.session.get("invocation-id").flatMap { invocationId =>
                        cache.get[MessageEvent](invocationId).map { event =>
                          eventHandler.handle(event, None).map { results =>
                            results.map(_.sendIn(event.context, None, None))
                            Redirect(routes.APIAccessController.authenticated(s"There should now be a response in ${event.context.name}."))
                          }
                        }
                      }.getOrElse {
                        val redirect = maybeRedirectAfterAuth.getOrElse {
                          routes.APIAccessController.authenticated(s"You are now authenticated and can try again.").toString
                        }
                        Future.successful(Redirect(redirect))
                      }
                    }.getOrElse(Future.successful(BadRequest("boom")))
                }
              } else {
                Future.successful(BadRequest("Invalid state"))
              }
            }).getOrElse {
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
          } else {
            reAuthFor(request, maybeApplication.map(_.teamId))
          }
        }
      }.getOrElse(Future.successful(NotFound(views.html.notFound(None, Some("Can't find OAuth2 application"), None, None))))
    } yield result
  }

  def authenticated(message: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    dataService.teams.find(user.teamId).map { maybeTeam =>
      Ok(views.html.authenticated(maybeTeam, message))
    }
  }


}
