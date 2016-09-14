package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json._
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService, GithubService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val silhouette: Silhouette[EllipsisEnv],
                                        val configuration: Configuration,
                                        val dataService: DataService,
                                        val lambdaService: AWSLambdaService,
                                        val ws: WSClient,
                                        val cache: CacheApi
                                      ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, maybeTeamId))
      maybeBehaviors <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.from(dataService.behaviors.allForTeam(team)).map { behaviors =>
          Some(behaviors)
        }
      }.getOrElse {
        DBIO.successful(None)
      }
      versionData <- DBIO.sequence(maybeBehaviors.map { behaviors =>
        behaviors.map { behavior =>
          BehaviorVersionData.maybeFor(behavior.id, user, dataService)
        }
      }.getOrElse(Seq())).map(_.flatten)
      result <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.successful(if (versionData.isEmpty) {
          Redirect(routes.ApplicationController.intro(maybeTeamId))
        } else {
          Ok(views.html.index(teamAccess, versionData))
        })
      }.getOrElse {
        DBIO.from(reAuthFor(request, maybeTeamId))
      }
    } yield result

    dataService.run(action)
  }

  case class PublishedBehaviorInfo(published: Seq[BehaviorCategory], installedBehaviors: Seq[InstalledBehaviorData])

  private def withPublishedBehaviorInfoFor(team: Team): Future[PublishedBehaviorInfo] = {
    dataService.behaviors.allForTeam(team).map { behaviors =>
      behaviors.map { ea => InstalledBehaviorData(ea.id, ea.maybeImportedId)}
    }.map { installedBehaviors =>
      val githubService = GithubService(team, ws, configuration, cache, dataService)
      PublishedBehaviorInfo(githubService.publishedBehaviorCategories, installedBehaviors)
    }
  }

  def intro(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          Future.successful(
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
  }

  def installBehaviors(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          Future.successful(
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
  }

}
