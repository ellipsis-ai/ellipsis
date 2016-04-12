package controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.Clock
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

  def authenticateSlack(
                         maybeRedirect: Option[String],
                         maybeTeamId: Option[String],
                         maybeChannelId: Option[String]
                         ) = UserAwareAction.async { implicit request =>
    val isHttps = configuration.getBoolean("application.https").getOrElse(true)
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.authenticateSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = isHttps)
      val authorizationParams = maybeTeamId.map { teamId =>
        settings.authorizationParams + ("team" -> teamId)
      }.getOrElse(settings.authorizationParams)
      settings.copy(redirectURL = url, authorizationParams = authorizationParams)
    }
    val authenticateResult = provider.authenticate() recover {
      case e: com.mohiva.play.silhouette.impl.exceptions.AccessDeniedException => {
        Left(Redirect(routes.ApplicationController.addToSlack))
      }
      case e: com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException => {
        Left(Redirect(routes.ApplicationController.index))
      }
    }
    authenticateResult.flatMap {
      case Left(result) => Future.successful(result)
      case Right(authInfo) => {
        for {
          profile <- slackProvider.retrieveProfile(authInfo)
          maybeSlackTeamId <- Future.successful(Some(maybeTeamId.getOrElse(profile.teamId)))
          savedProfile <- Models.run(SlackProfileQueries.save(profile))
          authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
          maybeExistingLinkedAccount <- Models.run(LinkedAccount.find(profile.loginInfo))
          linkedAccount <- maybeExistingLinkedAccount.map(Future.successful).getOrElse {
            request.identity.map(Future.successful).getOrElse(Models.run(User.empty.save)).flatMap { user =>
              Models.run(LinkedAccount(user, profile.loginInfo, DateTime.now).save)
            }
          }
          user <- Future.successful(linkedAccount.user)
          result <- Future.successful(Ok("Good job!"))
          authenticatedResult <- Models.run(authenticatorResultForUserAndResult(user, result))
        } yield {
          authenticatedResult
        }
      }
    }
  }
}
