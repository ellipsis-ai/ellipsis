package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.{IDs, Models, Team}
import models.accounts.{LinkedOAuth2Token, OAuth2Application, OAuth2ApplicationQueries, User}
import models.bots.events.{EventHandler, MessageEvent}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.http.{HeaderNames, MimeTypes}
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
                                        val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  private def getToken(code: String, application: OAuth2Application, user: User, redirectUrl: String): DBIO[Option[LinkedOAuth2Token]] = {
    val tokenResponse =
      application.accessTokenRequestFor(code, redirectUrl, ws).
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
        LinkedOAuth2Token(accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, maybeScopeGranted, user.id, application).save.map(Some(_))
      }.getOrElse(DBIO.successful(None))

    }
  }

  def linkCustomOAuth2Service(
                               applicationId: String,
                               codeOpt: Option[String],
                               stateOpt: Option[String],
                               maybeInvocationId: Option[String]
                               ) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeApplication <- OAuth2ApplicationQueries.find(applicationId)
      isLoggedInToCorrectTeam <- maybeApplication.map { application =>
        Team.find(application.teamId, user).map(_.isDefined)
      }.getOrElse(DBIO.successful(false))
      result <- if (isLoggedInToCorrectTeam) {
        (for {
          application <- maybeApplication
          code <- codeOpt
          state <- stateOpt
          oauthState <- request.session.get("oauth-state")
        } yield {
            if (state == oauthState) {
              val redirect = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, maybeInvocationId).absoluteURL(secure=true)
              getToken(code, application, user, redirect).flatMap { maybeLinkedToken =>
                maybeLinkedToken.
                  map { _ =>
                  request.session.get("invocation-id").flatMap { invocationId =>
                    cache.get[MessageEvent](invocationId).map { event =>
                      DBIO.from(eventHandler.handle(event)).map { result =>
                        result.sendIn(event.context)
                        Redirect(routes.APIAccessController.authenticated(s"There should now be a response in ${event.context.name}."))
                      }
                    }
                  }.getOrElse {
                    DBIO.successful(Redirect(routes.APIAccessController.authenticated(s"You are now authenticated and can try again.")))
                  }
                }.getOrElse(DBIO.successful(BadRequest("boom")))
              }
            } else {
              DBIO.successful(BadRequest("Invalid state"))
            }
          }).getOrElse {
          maybeApplication.map { application =>
            val state = IDs.next
            val redirectParam = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None).absoluteURL(secure=true)
            val redirect = application.authorizationRequestFor(state, redirectParam, ws).uri.toString
            val sessionState = Seq(Some("oauth-state" -> state), maybeInvocationId.map(id => "invocation-id" -> id)).flatten
            DBIO.successful(Redirect(redirect).withSession(sessionState: _*))
          }.getOrElse(DBIO.successful(NotFound("Can't find OAuth2 application")))
        }
      } else {
        reAuthFor(request, maybeApplication.map(_.teamId))
      }
    } yield result

    models.run(action)
  }

  def authenticated(message: String) = SecuredAction { implicit request =>
    Ok(views.html.authenticated(message))
  }


}
