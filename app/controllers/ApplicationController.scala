package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService, GithubService}
import utils.{CitiesToTimeZones, FuzzyMatcher, TimeZoneParser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val silhouette: Silhouette[EllipsisEnv],
                                        val configuration: Configuration,
                                        val dataService: DataService,
                                        val lambdaService: AWSLambdaService,
                                        val ws: WSClient,
                                        val cache: CacheApi,
                                        val githubService: GithubService,
                                        val citiesToTimeZones: CitiesToTimeZones
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
        Future.successful(Ok(views.html.index(viewConfig(Some(teamAccess)), groupData, maybeSlackTeamId, team.maybeTimeZone.map(_.toString), maybeBranch)))
      }.getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result
  }

  def fetchPublishedBehaviorInfo(maybeTeamId: Option[String],
                                    maybeBranch: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      alreadyInstalled <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorGroups.allFor(team)
      }.getOrElse(Future.successful(Seq()))
      alreadyInstalledData <- Future.sequence(alreadyInstalled.map { group =>
        BehaviorGroupData.maybeFor(group.id, user, None, dataService)
      }).map(_.flatten)
      result <- teamAccess.maybeTargetTeam.map { team =>
        Future.successful(Ok(Json.toJson(githubService.publishedBehaviorGroupsFor(team, maybeBranch, alreadyInstalledData))))
      }.getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result
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
            case Accepts.Json() => Ok(Json.toJson("deleted"))
          }
        }
      }
    )
  }

  def findBehaviorGroupsMatching(queryString: String, maybeBranch: Option[String], maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(maybeTeamId.getOrElse(user.teamId)))
      maybeInstalledBehaviorGroups <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorGroups.allFor(team).map(Some(_))
      }.getOrElse {
        Future.successful(None)
      }
      maybeInstalledGroupData <- maybeInstalledBehaviorGroups.map { groups =>
        val eventualMaybeGroupData = groups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, None, dataService)
        }
        Future.sequence(eventualMaybeGroupData).map { maybeGroups =>
          Some(maybeGroups.flatten.sorted)
        }
      }.getOrElse {
        Future.successful(None)
      }
    } yield {
      maybeInstalledGroupData.map { installedGroupData =>
        val publishedGroupData = teamAccess.maybeTargetTeam.map { team =>
          githubService.publishedBehaviorGroupsFor(team, maybeBranch, installedGroupData)
        }.getOrElse(Seq())
        val matchResults = FuzzyMatcher[BehaviorGroupData](queryString, installedGroupData ++ publishedGroupData).run
        Ok(Json.toJson(matchResults.map(_.item)).toString)
      }.getOrElse {
        NotFound("")
      }
    }
  }

  def possibleTimeZonesFor(searchQuery: String) = silhouette.SecuredAction { implicit request =>
    val results = citiesToTimeZones.possibleCitiesFor(searchQuery)
    Ok(Json.obj("results" -> results))
  }

  private val timeZoneForm = Form(
    mapping(
      "tzName" -> nonEmptyText,
      "teamId" -> optional(nonEmptyText)
    )(TeamTimeZoneData.apply)(TeamTimeZoneData.unapply)
  )

  def setTeamTimeZone = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    timeZoneForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, Some(info.maybeTeamId.getOrElse(user.teamId)))
          maybeTeam <- teamAccess.maybeTargetTeam.map { team =>
            TimeZoneParser.maybeZoneFor(info.tzName).map { tz =>
              dataService.teams.setTimeZoneFor(team, tz).map(Some(_))
            }.getOrElse(Future.successful(Some(team)))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeTeam.map { team =>
            team.maybeTimeZone.map { tz =>
              Ok(Json.toJson(TeamTimeZoneData(tz.toString, None)).toString)
            }.getOrElse {
              BadRequest(Json.obj("message" -> "Invalid time zone").toString)
            }
          }.getOrElse {
            NotFound("")
          }
        }
      }
    )
  }

}
