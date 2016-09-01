package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json._
import models._
import models.accounts._
import models.accounts.user.User
import models.bots._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, GithubService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
                                        val lambdaService: AWSLambdaService,
                                        val ws: WSClient,
                                        val cache: CacheApi,
                                        val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  def index(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybeBehaviors <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorQueries.allForTeam(team).map { behaviors =>
          Some(behaviors)
        }
      }.getOrElse {
        DBIO.successful(None)
      }
      versionData <- DBIO.sequence(maybeBehaviors.map { behaviors =>
        behaviors.map { behavior =>
          BehaviorVersionData.maybeFor(behavior.id, user)
        }
      }.getOrElse(Seq())).map(_.flatten)
      result <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.successful(if (versionData.isEmpty) {
          Redirect(routes.ApplicationController.intro(maybeTeamId))
        } else {
          Ok(views.html.index(teamAccess, versionData))
        })
      }.getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

  case class PublishedBehaviorInfo(published: Seq[BehaviorCategory], installedBehaviors: Seq[InstalledBehaviorData])

  private def withPublishedBehaviorInfoFor(team: Team): DBIO[PublishedBehaviorInfo] = {
    BehaviorQueries.allForTeam(team).map { behaviors =>
      behaviors.map { ea => InstalledBehaviorData(ea.id, ea.maybeImportedId)}
    }.map { installedBehaviors =>
      val githubService = GithubService(team, ws, configuration, cache)
      PublishedBehaviorInfo(githubService.publishedBehaviorCategories, installedBehaviors)
    }
  }

  def intro(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          DBIO.successful(
            Ok(
              views.html.intro(
                teamAccess,
                data.published,
                data.installedBehaviors
              )
            )
          )
        }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

  def installBehaviors(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          DBIO.successful(
            Ok(
              views.html.publishedBehaviors(
                teamAccess,
                data.published,
                data.installedBehaviors
              )
            )
          )
        }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

}
