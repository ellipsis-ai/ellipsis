package controllers

import java.net.{URLEncoder, URI}
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.Clock
import models.accounts._
import org.joda.time.DateTime
import play.api.Configuration
import services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import models._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{RequestHeader, Result}
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

  private def isValidRouteForRedirect(uri: URI): Boolean = {
    uri.getPath != new URI(routes.SocialAuthController.signOut().url).getPath
  }

  private def isForAnotherHost(uri: URI)(implicit r: RequestHeader): Boolean = {
    uri.isAbsolute && uri.getHost != r.host.split(":").head
  }

  private def validatedRedirectUri(uri: String)(implicit r: RequestHeader): String = {
    val parsed = new URI(uri)
    if (isForAnotherHost(parsed) || !isValidRouteForRedirect(parsed)) {
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
          linkedAccount <- models.run(LinkedAccount.find(profile.loginInfo, botProfile.teamId).flatMap { maybeExisting =>
            maybeExisting.map(DBIO.successful).getOrElse {
              val eventualUser = DBIO.from(request.identity.map(Future.successful).getOrElse {
                userService.createFor(botProfile.teamId)
              })
              eventualUser.flatMap { user =>
                LinkedAccount(user, profile.loginInfo, DateTime.now).save
              }
            }
          })
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
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.authenticateSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      var authorizationParams = settings.authorizationParams
      maybeTeamId.foreach { teamId =>
        authorizationParams = authorizationParams + ("team" -> teamId)
      }
      configuration.getString("silhouette.slack.signInScope").foreach { signInScope =>
        authorizationParams = authorizationParams + ("scope" -> signInScope)
      }
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
          result <- if (maybeTeamId.exists(t => t != teamId)) {
            Future.successful {
              val redir = s"/oauth/authorize?client_id=${provider.settings.clientID}&redirect_url=${provider.settings.redirectURL}&scope=${provider.settings.authorizationParams.get("scope").get}"
              val url = s"https://slack.com/signin?redir=${URLEncoder.encode(redir, "UTF-8")}"
              Redirect(url)
            }
          } else {
            for {
              savedProfile <- models.run(SlackProfileQueries.save(profile))
              loginInfo <- Future.successful(profile.loginInfo)
              savedAuthInfo <- authInfoRepository.save(loginInfo, authInfo)
              maybeExistingLinkedAccount <- models.run(LinkedAccount.find(profile.loginInfo, teamId))
              linkedAccount <- models.run(
                maybeExistingLinkedAccount.map(DBIO.successful).getOrElse {
                  val eventualUser = DBIO.from(request.identity.map(Future.successful).getOrElse {
                    userService.createFor(teamId)
                  })
                  eventualUser.flatMap { user =>
                    LinkedAccount(user, profile.loginInfo, DateTime.now).save
                  }
                }
              )
              user <- Future.successful(linkedAccount.user)
              result <- Future.successful {
                maybeRedirect.map { redirect =>
                  Redirect(validatedRedirectUri(redirect))
                }.getOrElse(Redirect(routes.ApplicationController.index()))
              }
              authenticatedResult <- models.run(authenticatorResultForUserAndResult(user, result))
            } yield authenticatedResult
          }
        } yield result
      }
    }
  }

  def loginWithToken(
            token: String,
            maybeRedirect: Option[String]
            ) = UserAwareAction.async { implicit request =>
    val successRedirect = validatedRedirectUri(maybeRedirect.getOrElse(routes.ApplicationController.index().toString))
    request.identity.map { user =>
      Future.successful(Redirect(successRedirect))
    }.getOrElse {
      val action = for {
        maybeToken <- LoginTokenQueries.find(token)
        result <- maybeToken.map { token =>
          if (token.isValid) {
            for {
              _ <- token.use
              maybeUser <- User.find(token.userId)
              resultForValidToken <- maybeUser.map { user =>
                authenticatorResultForUserAndResult(user, Redirect(successRedirect))
              }.getOrElse {
                DBIO.successful(Redirect(routes.SlackController.signIn(maybeRedirect)))
              }
            } yield resultForValidToken
          } else {
            DBIO.successful(Ok(views.html.loginTokenExpired()))
          }
        }.getOrElse {
          DBIO.successful(NotFound("Token not found"))
        }
      } yield result

      models.run(action)
    }
  }

  def signOut = SecuredAction.async { implicit request =>
    val redirect = request.request.headers.get("referer").getOrElse(routes.ApplicationController.index().toString)
    env.authenticatorService.discard(request.authenticator, Redirect(redirect))
  }

}
