package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import json._
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Result}
import services.{AWSLambdaService, DataService, GithubService}

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

  import json.Formatting._

  def index(maybeTeamId: Option[String], maybeBranch: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeBehaviorGroups <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorGroups.allFor(team).map(Some(_))
      }.getOrElse {
        Future.successful(None)
      }
      maybeSlackTeamId <- teamAccess.maybeTargetTeam.map { team =>
        dataService.slackBotProfiles.allFor(team).map { botProfiles =>
          botProfiles.headOption.map(_.slackTeamId)
        }
      }.getOrElse(Future.successful(None))
      groupData <- maybeBehaviorGroups.map { groups =>
        Future.sequence(groups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, None, dataService)
        }).map(_.flatten.sorted)
      }.getOrElse(Future.successful(Seq()))
      result <- teamAccess.maybeTargetTeam.map { team =>
        Future.successful(if (groupData.isEmpty) {
          Redirect(routes.ApplicationController.intro(maybeTeamId))
        } else {
          Ok(views.html.index(viewConfig(Some(teamAccess)), groupData, maybeSlackTeamId, maybeBranch))
        })
      }.getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result
  }

  case class PublishedBehaviorInfo(published: Seq[BehaviorGroupData], installedBehaviors: Seq[InstalledBehaviorGroupData])

  private def withPublishedBehaviorInfoFor(team: Team, maybeBranch: Option[String]): Future[PublishedBehaviorInfo] = {
    dataService.behaviorGroups.allFor(team).map { groups =>
      groups.map { ea => InstalledBehaviorGroupData(ea.id, ea.maybeExportId)}
    }.map { installedGroups =>
      val githubService = GithubService(team, ws, configuration, cache, dataService, maybeBranch)
      PublishedBehaviorInfo(githubService.publishedBehaviorGroups, installedGroups)
    }
  }

  def fetchPublishedBehaviorInfo(maybeTeamId: Option[String],
                                    maybeBranch: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team, maybeBranch).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        _ <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
        Future.successful(Ok(Json.toJson(data.published)))
      }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result
  }

  def installBehaviorGroupsWithView(
                                     maybeTeamId: Option[String],
                                     maybeBranch: Option[String] = None,
                                     resultFn: (UserTeamAccess, PublishedBehaviorInfo, Option[String]) => Result
                                   )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]) = {
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team, maybeBranch).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeSlackTeamId <- teamAccess.maybeTargetTeam.map { team =>
        dataService.slackBotProfiles.allFor(team).map { botProfiles =>
          botProfiles.headOption.map(_.slackTeamId)
        }
      }.getOrElse(Future.successful(None))
      result <- (for {
        _ <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
        Future.successful(
          resultFn(teamAccess, data, maybeSlackTeamId)
        )
      }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result
  }


  def intro(maybeTeamId: Option[String], maybeBranch: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    installBehaviorGroupsWithView(maybeTeamId, maybeBranch, (teamAccess, data, maybeSlackTeamId) => {
      Ok(
        views.html.intro(
          viewConfig(Some(teamAccess)),
          data.published,
          data.installedBehaviors,
          maybeSlackTeamId
        )
      )
    })
  }

  case class SelectedBehaviorGroupsInfo(behaviorGroupIds: Seq[String])

  private val selectedBehaviorGroupsForm = Form(
    mapping(
      "behaviorGroupIds" -> seq(nonEmptyText)
    )(SelectedBehaviorGroupsInfo.apply)(SelectedBehaviorGroupsInfo.unapply)
  )

  def mergeBehaviorGroups = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    selectedBehaviorGroupsForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          groups <- Future.sequence(info.behaviorGroupIds.map { id =>
            dataService.behaviorGroups.find(id)
          }).map(_.flatten)
          merged <- dataService.behaviorGroups.merge(groups, user)
          maybeData <- BehaviorGroupData.maybeFor(merged.id, user, None, dataService)
        } yield maybeData.map { data =>
          Ok(Json.toJson(data))
        }.getOrElse {
          NotFound("Merged skill not found")
        }
      }
    )
  }

  def deleteBehaviorGroups = silhouette.SecuredAction.async { implicit request =>
    selectedBehaviorGroupsForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          groups <- Future.sequence(info.behaviorGroupIds.map { id =>
            dataService.behaviorGroups.find(id)
          }).map(_.flatten)
          deleted <- Future.sequence(groups.map(dataService.behaviorGroups.delete))
        } yield {
          render {
            case Accepts.Html() => Redirect(routes.ApplicationController.index())
            case Accepts.Json() => Ok("deleted")
          }
        }
      }
    )
  }

}
