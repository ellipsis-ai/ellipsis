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
import utils.{FuzzyMatcher, TimeZoneParser}

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
                                        val githubService: GithubService
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
      result <- teamAccess.maybeTargetTeam.map { team =>
        Future.successful(Ok(Json.toJson(githubService.publishedBehaviorGroupsFor(team, maybeBranch))))
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
          githubService.publishedBehaviorGroupsFor(team, maybeBranch)
        }.getOrElse(Seq())
        val matchResults = FuzzyMatcher[BehaviorGroupData](queryString, installedGroupData ++ publishedGroupData).run
        Ok(Json.toJson(matchResults.map(_.item)).toString)
      }.getOrElse {
        NotFound("")
      }
    }
  }

  case class UpdateBehaviorGroupInfo(dataJson: String)

  private val updateForm = Form(
    mapping(
      "dataJson" -> nonEmptyText
    )(UpdateBehaviorGroupInfo.apply)(UpdateBehaviorGroupInfo.unapply)
  )

  def updateBehaviorGroup = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    updateForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorGroupData] match {
          case JsSuccess(data, jsPath) => {
            for {
              teamAccess <- dataService.users.teamAccessFor(user, Some(data.teamId))
              maybeExistingGroup <- data.id.map { groupId =>
                dataService.behaviorGroups.find(groupId)
              }.getOrElse(Future.successful(None))
              maybeGroup <- maybeExistingGroup.map(g => Future.successful(Some(g))).getOrElse {
                teamAccess.maybeTargetTeam.map { team =>
                  dataService.behaviorGroups.createFor(data.exportId, team).map(Some(_))
                }.getOrElse(Future.successful(None))
              }
              maybeGroupData <- maybeGroup.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, maybeGithubUrl = None, dataService)
              }.getOrElse(Future.successful(None))
              _ <- (for {
                group <- maybeGroup
                groupData <- maybeGroupData
              } yield {
                dataService.behaviorGroupVersions.createFor(group, user, data.copyForUpdateOf(groupData)).map(Some(_))
              }).getOrElse(Future.successful(None))
              maybeGroupData <- maybeGroup.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, maybeGithubUrl = None, dataService)
              }.getOrElse(Future.successful(None))
            } yield {
              maybeGroupData.map { groupData =>
                Ok(Json.toJson(groupData))
              }.getOrElse {
                NotFound("")
              }
            }
          }
          case e: JsError => {
            Future.successful(BadRequest(s"Malformatted data: ${e.errors.mkString("\n")}"))
          }
        }
      }
    )
  }

  case class SetTeamTimeZoneInfo(tzName: String, maybeTeamId: Option[String])

  private val timeZoneForm = Form(
    mapping(
      "tzName" -> nonEmptyText,
      "teamId" -> optional(nonEmptyText)
    )(SetTeamTimeZoneInfo.apply)(SetTeamTimeZoneInfo.unapply)
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
              Ok(Json.toJson(tz.toString).toString)
            }.getOrElse {
              BadRequest("Invalid time zone")
            }
          }.getOrElse {
            NotFound("")
          }
        }
      }
    )
  }

}
