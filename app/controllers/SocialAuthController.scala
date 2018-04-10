package controllers

import java.net.{URI, URLDecoder, URLEncoder}
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.util.Clock
import play.api.Configuration
import services.DataService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models._
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.github.GithubProvider
import models.accounts.slack.SlackProvider
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class SocialAuthController @Inject() (
                                       val silhouette: Silhouette[EllipsisEnv],
                                       val configuration: Configuration,
                                       val clock: Clock,
                                       val models: Models,
                                       val assetsProvider: Provider[RemoteAssets],
                                       slackProvider: SlackProvider,
                                       githubProvider: GithubProvider,
                                       dataService: DataService,
                                       authInfoRepository: AuthInfoRepository,
                                       implicit val ec: ExecutionContext
                                     ) extends EllipsisController with Logger {

  val env = silhouette.env

  def authenticatorResultForUserAndResult(user: User, result: Result)(implicit request: RequestHeader): Future[AuthenticatorResult] = {
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
      env.eventBus.publish(LoginEvent(user, request))
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
                         ) = silhouette.UserAwareAction.async { implicit request =>
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.installForSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      val authorizationParams = maybeTeamId.map { teamId =>
        settings.authorizationParams + ("team" -> teamId)
      }.getOrElse(settings.authorizationParams)
      settings.copy(redirectURL = Some(url), authorizationParams = authorizationParams)
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
          botProfile <- slackProvider.maybeBotProfileFor(authInfo, dataService).map { maybeBotProfile =>
            maybeBotProfile.get // Blow up if we can't get a bot profile
          }
          _ <- authInfoRepository.save(profile.loginInfo, authInfo)
          linkedAccount <- dataService.linkedAccounts.find(profile.loginInfo, botProfile.teamId).flatMap { maybeExisting =>
            maybeExisting.map(Future.successful).getOrElse {
              val eventualUser = request.identity.map(Future.successful).getOrElse {
                dataService.users.createFor(botProfile.teamId)
              }
              eventualUser.flatMap { user =>
                dataService.linkedAccounts.save(LinkedAccount(user, profile.loginInfo, OffsetDateTime.now))
              }
            }
          }
          user <- Future.successful(linkedAccount.user)
          result <- Future.successful {
            maybeRedirect.map { redirect =>
              Redirect(validatedRedirectUri(redirect))
            }.getOrElse(Redirect(routes.ApplicationController.index()))
          }
          authenticatedResult <- authenticatorResultForUserAndResult(user, result)
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
                         ) = silhouette.UserAwareAction.async { implicit request =>
    val provider = slackProvider.withSettings { settings =>
      val url = routes.SocialAuthController.authenticateSlack(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      var authorizationParams = settings.authorizationParams
      maybeTeamId.foreach { teamId =>
        authorizationParams = authorizationParams + ("team" -> teamId)
      }
      configuration.getOptional[String]("silhouette.slack.signInScope").foreach { signInScope =>
        authorizationParams = authorizationParams + ("scope" -> signInScope)
      }
      settings.copy(redirectURL = Some(url), authorizationParams = authorizationParams)
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
          botProfiles <- dataService.slackBotProfiles.allForSlackTeamId(profile.teamId)
          result <- if (botProfiles.isEmpty) {
            Future.successful(Redirect(routes.SocialAuthController.installForSlack(maybeRedirect, maybeTeamId, maybeChannelId)))
          } else {
            val teamId = botProfiles.head.teamId
            if (maybeTeamId.exists(t => t != teamId)) {
              Future.successful {
                val redir = s"/oauth/authorize?client_id=${provider.settings.clientID}&redirect_url=${provider.settings.redirectURL}&scope=${provider.settings.authorizationParams("scope")}"
                val url = s"https://slack.com/signin?redir=${URLEncoder.encode(redir, "UTF-8")}"
                Redirect(url)
              }
            } else {
              for {
                loginInfo <- Future.successful(profile.loginInfo)
                _ <- authInfoRepository.save(loginInfo, authInfo)
                maybeExistingLinkedAccount <- dataService.linkedAccounts.find(profile.loginInfo, teamId)
                linkedAccount <- maybeExistingLinkedAccount.map(Future.successful).getOrElse {
                  val eventualUser = request.identity.map(Future.successful).getOrElse {
                    dataService.users.createFor(teamId)
                  }
                  eventualUser.flatMap { user =>
                    dataService.linkedAccounts.save(LinkedAccount(user, profile.loginInfo, OffsetDateTime.now))
                  }
                }
                user <- Future.successful(linkedAccount.user)
                result <- Future.successful {
                  maybeRedirect.map { redirect =>
                    Redirect(validatedRedirectUri(redirect))
                  }.getOrElse(Redirect(routes.ApplicationController.index()))
                }
                authenticatedResult <- authenticatorResultForUserAndResult(user, result)
              } yield authenticatedResult
            }
          }
        } yield result
      }
    }
  }

  def authenticateGithub(
                         maybeRedirect: Option[String],
                         maybeTeamId: Option[String],
                         maybeChannelId: Option[String]
                       ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val provider = githubProvider.withSettings { settings =>
      val url = routes.SocialAuthController.authenticateGithub(maybeRedirect, maybeTeamId, maybeChannelId).absoluteURL(secure = true)
      var authorizationParams = settings.authorizationParams
      configuration.getOptional[String]("silhouette.github.scope").foreach { signInScope =>
        authorizationParams = authorizationParams + ("scope" -> signInScope)
      }
      settings.copy(redirectURL = Some(url), authorizationParams = authorizationParams)
    }
    val authenticateResult = provider.authenticate() recover {
      case e: com.mohiva.play.silhouette.api.exceptions.ProviderException => {
        val redirect = maybeRedirect.getOrElse(routes.ApplicationController.index().url)
        Left(Redirect(validatedRedirectUri(redirect)))
      }
    }
    authenticateResult.flatMap {
      case Left(result) => Future.successful(result)
      case Right(authInfo) => for {
        profile <- githubProvider.retrieveProfile(authInfo)
        result <- for {
          _ <- dataService.githubProfiles.save(profile)
          _ <- authInfoRepository.save(profile.loginInfo, authInfo)
          maybeExistingLinkedAccount <- dataService.linkedAccounts.find(profile.loginInfo, user.teamId)
          _ <- maybeExistingLinkedAccount.map(Future.successful).getOrElse {
            dataService.linkedAccounts.save(LinkedAccount(user, profile.loginInfo, OffsetDateTime.now))
          }
          result <- Future.successful {
            maybeRedirect.map { redirect =>
              Redirect(validatedRedirectUri(redirect))
            }.getOrElse(Redirect(routes.ApplicationController.index()))
          }
          authenticatedResult <- authenticatorResultForUserAndResult(user, result)
        } yield authenticatedResult
      } yield result
    }
  }

  def loginWithToken(
            token: String,
            maybeRedirect: Option[String]
            ) = silhouette.UserAwareAction.async { implicit request =>
    val successRedirect = validatedRedirectUri(
      maybeRedirect.
        map(URLDecoder.decode(_, "utf-8")).
        getOrElse(routes.ApplicationController.index().toString)
    )
    for {
      maybeToken <- dataService.loginTokens.find(token)
      result <- maybeToken.map { token =>
        val isAlreadyLoggedInAsTokenUser = request.identity.exists(_.id == token.userId)
        if (isAlreadyLoggedInAsTokenUser) {
          Future.successful(Redirect(successRedirect))
        } else if (token.isValid) {
          for {
            maybeUser <- dataService.users.find(token.userId)
            resultForValidToken <- maybeUser.map { user =>
              authenticatorResultForUserAndResult(user, Redirect(successRedirect))
            }.getOrElse {
              Future.successful(Redirect(routes.SlackController.signIn(maybeRedirect)))
            }
          } yield resultForValidToken
        } else {
          Future.successful(Ok(views.html.socialauth.loginTokenExpired(viewConfig(None))))
        }
      }.getOrElse {
        Future.successful(NotFound("Token not found"))
      }
    } yield result
  }

  def signOut = silhouette.SecuredAction.async { implicit request =>
    val redirect = request.request.headers.get("referer").getOrElse(routes.ApplicationController.index().toString)
    env.authenticatorService.discard(request.authenticator, Redirect(redirect))
  }

}
