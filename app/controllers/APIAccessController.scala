package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Silhouette, Environment}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.bots.{MessageEvent, EventHandler}
import models.{IDs, Team, Models}
import models.accounts.{OAuth2ApplicationQueries, LinkedOAuth2Token, OAuth2Application, User}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.http.{MimeTypes, HeaderNames}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Results
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class APIAccessController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
                                        val ws: WSClient,
                                        val cache: CacheApi,
                                        val eventHandler: EventHandler,
                                        socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  private def getToken(code: String, authConfig: OAuth2Application, user: User, redirectUrl: String): DBIO[Option[LinkedOAuth2Token]] = {
    val tokenResponse =
      authConfig.accessTokenRequestFor(code, redirectUrl, ws).
        withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
        post(Results.EmptyContent())

    DBIO.from(tokenResponse).flatMap { response =>
      val json = response.json
      (json \ "access_token").asOpt[String].map { accessToken =>
        val maybeTokenType = (json \ "token_type").asOpt[String]
        val maybeScopeGranted = (json \ "scope").asOpt[String]
        val maybeExpirationTime = (json \ "expires_in").asOpt[Int].map { seconds =>
          DateTime.now.plusSeconds(seconds)
        }
        val maybeRefreshToken = (json \ "refresh_token").asOpt[String]
        LinkedOAuth2Token(accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, maybeScopeGranted, user.id, authConfig).save.map(Some(_))
      }.getOrElse(DBIO.successful(None))

    }
  }

  def linkCustomOAuth2Service(
                               configId: String,
                               codeOpt: Option[String],
                               stateOpt: Option[String],
                               maybeInvocationId: Option[String]
                               ) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeAuthConfig <- OAuth2ApplicationQueries.find(configId)
      maybeTeam <- maybeAuthConfig.map { config =>
        Team.find(config.teamId, user)
      }.getOrElse(DBIO.successful(None))
      result <- (for {
        authConfig <- maybeAuthConfig
        code <- codeOpt
        state <- stateOpt
        oauthState <- request.session.get("oauth-state")
      } yield {
          if (state == oauthState) {
            val redirect = routes.APIAccessController.linkCustomOAuth2Service(authConfig.id, None, None, maybeInvocationId).absoluteURL(secure=true)
            getToken(code, authConfig, user, redirect).map { maybeLinkedToken =>
              maybeLinkedToken.
                map { _ =>
                request.session.get("invocation-id").flatMap { invocationId =>
                    cache.get[MessageEvent](invocationId).map { event =>
                      eventHandler.handle(event)
                      Redirect(routes.APIAccessController.authenticated(s"There should now be a response in ${event.context.name}."))
                    }
                  }.getOrElse {
                    Redirect(routes.APIAccessController.authenticated(s"You are now authenticated and can try again."))
                  }
                }.
                getOrElse(BadRequest("boom"))
            }
          } else {
            DBIO.successful(BadRequest("Invalid state"))
          }
        }).getOrElse {
        maybeAuthConfig.map { authConfig =>
          val state = IDs.next
          val redirectParam = routes.APIAccessController.linkCustomOAuth2Service(authConfig.id, None, None, None).absoluteURL(secure=true)
          val redirect = authConfig.authorizationRequestFor(state, redirectParam, ws).uri.toString
          val sessionState = Seq(Some("oauth-state" -> state), maybeInvocationId.map(id => "invocation-id" -> id)).flatten
          DBIO.successful(Redirect(redirect).withSession(sessionState: _*))
        }.getOrElse(DBIO.successful(NotFound("Bad team/config")))
      }
    } yield result

    models.run(action)
  }

  def authenticated(message: String) = SecuredAction { implicit request =>
    Ok(views.html.authenticated(message))
  }


}
