package controllers

import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.Clock
import models.accounts._
import org.joda.time.DateTime
import play.api.Configuration
import services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import models._
import play.api.i18n.{ MessagesApi, Messages }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{RequestHeader, Result, Action}
import slick.driver.PostgresDriver.api._


import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SocialAuthController @Inject() (
                                       val messagesApi: MessagesApi,
                                       val env: Environment[User, CookieAuthenticator],
                                       val configuration: Configuration,
                                       val clock: Clock,
                                       val models: Models,
                                       slackProvider: SlackProvider,
                                       userService: UserService,
                                       authInfoRepository: AuthInfoRepository,
                                       socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] with Logger {

  def authenticatorResultForUserAndResult(user: User, result: Result)(implicit request: RequestHeader): DBIO[AuthenticatorResult] = DBIO.from {
    val c = configuration.underlying
    env.authenticatorService.create(user.loginInfo).map { (authenticator: CookieAuthenticator) =>
      val expirationTime = clock.now.plusSeconds(c.getDuration("silhouette.authenticator.authenticatorExpiry", TimeUnit.SECONDS).toInt)
      val idleTimeoutSeconds = c.getDuration("silhouette.authenticator.authenticatorIdleTimeout", TimeUnit.SECONDS)
      val cookieMaxAgeSeconds = c.getDuration("silhouette.authenticator.cookieMaxAge", TimeUnit.SECONDS)
      authenticator.copy(
        expirationDateTime = expirationTime,
        idleTimeout = Some(FiniteDuration(idleTimeoutSeconds, TimeUnit.SECONDS)),
        cookieMaxAge = Some(FiniteDuration(cookieMaxAgeSeconds, TimeUnit.SECONDS)))
    }.flatMap { authenticator =>
      env.eventBus.publish(LoginEvent(user, request, request2Messages))
      env.authenticatorService.init(authenticator).flatMap { v =>
        env.authenticatorService.embed(v, result)
      }
    }
  }

  private def validatedRedirectUri(uri: String)(implicit r: RequestHeader): String = {
    val parsed = new URI(uri)
    if (parsed.isAbsolute && parsed.getHost != r.host.split(":").head) {
      routes.ApplicationController.index().toString
    } else {
      uri
    }
  }

  def installForSlack(
                         maybeRedirect: Option[String],
                         maybeTeamId: Option[String],
                         maybeChannelId: Option[String]
                         ) = UserAwareAction.async { implicit request =>
    val isHttps = configuration.getBoolean("application.https").getOrElse(true)
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.installForSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      val authorizationParams = maybeTeamId.map { teamId =>
        settings.authorizationParams + ("team" -> teamId)
      }.getOrElse(settings.authorizationParams)
      settings.copy(redirectURL = url, authorizationParams = authorizationParams)
    }
    val authenticateResult = provider.authenticate() recover {
      case e: com.mohiva.play.silhouette.impl.exceptions.AccessDeniedException => {
        Left(Redirect(routes.SlackController.add()))
      }
      case e: com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException => {
        Left(Redirect(routes.ApplicationController.index()))
      }
    }
    authenticateResult.flatMap {
      case Left(result) => Future.successful(result)
      case Right(authInfo) => {
        for {
          profile <- slackProvider.retrieveProfile(authInfo)
          botProfile <- slackProvider.maybeBotProfileFor(authInfo, models).map { maybeBotProfile =>
            maybeBotProfile.get // Blow up if we can't get a bot profile
          }
          maybeSlackTeamId <- Future.successful(Some(maybeTeamId.getOrElse(profile.teamId)))
          savedProfile <- models.run(SlackProfileQueries.save(profile))
          savedAuthInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
          maybeExistingLinkedAccount <- models.run(LinkedAccount.find(profile.loginInfo))
          linkedAccount <- maybeExistingLinkedAccount.map(Future.successful).getOrElse {
            val eventualUser = request.identity.map(Future.successful).getOrElse {
              userService.createFor(botProfile.teamId)
            }
            eventualUser.flatMap { user =>
              models.run(LinkedAccount(user, profile.loginInfo, DateTime.now).save)
            }
          }
          user <- Future.successful(linkedAccount.user)
          result <- Future.successful {
            maybeRedirect.map { redirect =>
              Redirect(validatedRedirectUri(redirect))
            }.getOrElse(Redirect(routes.ApplicationController.index()))
          }
          authenticatedResult <- models.run(authenticatorResultForUserAndResult(user, result))
        } yield {
          authenticatedResult
        }
      }
    }
  }

  def authenticateSlack(
                         maybeRedirect: Option[String],
                         maybeTeamId: Option[String],
                         maybeChannelId: Option[String]
                         ) = UserAwareAction.async { implicit request =>
    val isHttps = configuration.getBoolean("application.https").getOrElse(true)
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.authenticateSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      val authorizationParams = maybeTeamId.map { teamId =>
        settings.authorizationParams + ("team" -> teamId)
      }.getOrElse(settings.authorizationParams)
      settings.copy(redirectURL = url, authorizationParams = authorizationParams)
    }
    val authenticateResult = provider.authenticate() recover {
      case e: com.mohiva.play.silhouette.impl.exceptions.AccessDeniedException => {
        Left(Redirect(routes.SlackController.signIn(maybeRedirect)))
      }
      case e: com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException => {
        Left(Redirect(routes.ApplicationController.index()))
      }
    }
    authenticateResult.flatMap {
      case Left(result) => Future.successful(result)
      case Right(authInfo) => {
        for {
          profile <- slackProvider.retrieveProfile(authInfo)
          teamId <- models.run(SlackBotProfileQueries.allForSlackTeamId(profile.teamId).map { botProfiles =>
            botProfiles.head.teamId // Blow up if no bot profile
          })
          savedProfile <- models.run(SlackProfileQueries.save(profile))
          loginInfo <- Future.successful(profile.loginInfo)
          savedAuthInfo <- authInfoRepository.save(loginInfo, authInfo)
          maybeExistingLinkedAccount <- models.run(LinkedAccount.find(loginInfo))
          linkedAccount <- maybeExistingLinkedAccount.map(Future.successful).getOrElse {
            request.identity.map(Future.successful).getOrElse(userService.createFor(teamId)).flatMap { user =>
              models.run(LinkedAccount(user, loginInfo, DateTime.now).save)
            }
          }
          user <- Future.successful(linkedAccount.user)
          result <- Future.successful {
            maybeRedirect.map { redirect =>
              Redirect(validatedRedirectUri(redirect))
            }.getOrElse(Redirect(routes.ApplicationController.index()))
          }
          authenticatedResult <- models.run(authenticatorResultForUserAndResult(user, result))
        } yield {
          authenticatedResult
        }
      }
    }
  }

  def signOut = SecuredAction.async { implicit request =>
    val redirect = request.request.headers.get("referer").getOrElse(routes.ApplicationController.index().toString)
    env.authenticatorService.discard(request.authenticator, Redirect(redirect))
  }

}
