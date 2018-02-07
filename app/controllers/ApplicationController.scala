package controllers

import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.{DefaultServices, GithubService}
import utils.github.GithubPublishedBehaviorGroupsFetcher
import utils.{CitiesToTimeZones, FuzzyMatcher, TimeZoneParser}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject() (
                                        val silhouette: Silhouette[EllipsisEnv],
                                        val githubService: GithubService,
                                        val services: DefaultServices,
                                        val citiesToTimeZones: CitiesToTimeZones,
                                        val assetsProvider: Provider[RemoteAssets],
                                        implicit val ec: ExecutionContext
                                      ) extends ReAuthable {

  import json.Formatting._

  val configuration = services.configuration
  val dataService = services.dataService
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws

  def teamHome(id: String, maybeBranch: Option[String] = None) = {
     index(Option(id), maybeBranch)
  }

  def index(maybeTeamId: Option[String], maybeBranch: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val eventualTeamAccess = dataService.users.teamAccessFor(user, maybeTeamId)
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- eventualTeamAccess
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
              BehaviorGroupData.maybeFor(group.id, user, None, dataService, cacheService)
            }).map(_.flatten.sorted)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = ApplicationIndexConfig(
              containerId = "behaviorListContainer",
              behaviorGroups = groupData,
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              slackTeamId = maybeSlackTeamId,
              teamTimeZone = team.maybeTimeZone.map(_.toString),
              branchName = maybeBranch
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "BehaviorListConfig",
              "behaviorList",
              Json.toJson(config)
            ))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- eventualTeamAccess
        } yield teamAccess.maybeTargetTeam.map { team =>
          Ok(views.html.application.index(viewConfig(Some(teamAccess)), maybeTeamId, maybeBranch))
        }.getOrElse {
          notFoundWithLoginFor(
            request,
            Some(teamAccess)
          )
        }
      }
    }
  }

  def fetchPublishedBehaviorInfo(
                                  maybeTeamId: Option[String],
                                  maybeBranch: Option[String] = None
                                ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      alreadyInstalled <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorGroups.allFor(team)
      }.getOrElse(Future.successful(Seq()))
      alreadyInstalledData <- Future.sequence(alreadyInstalled.map { group =>
        BehaviorGroupData.maybeFor(group.id, user, None, dataService, cacheService)
      }).map(_.flatten)
    } yield teamAccess.maybeTargetTeam.map { team =>
      val fetcher = GithubPublishedBehaviorGroupsFetcher(team, maybeBranch, alreadyInstalledData, githubService, services, ec)
      Ok(Json.toJson(fetcher.result))
    }.getOrElse {
      val message = maybeTeamId.map { teamId =>
        s"You can't access this for team ${teamId}"
      }.getOrElse {
        "You can't access this"
      }
      Forbidden(message)
    }
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
            dataService.behaviorGroups.findWithoutAccessCheck(id)
          }).map(_.flatten)
          merged <- dataService.behaviorGroups.merge(groups, user)
          maybeData <- BehaviorGroupData.maybeFor(merged.id, user, None, dataService, cacheService)
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
            dataService.behaviorGroups.findWithoutAccessCheck(id)
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
          BehaviorGroupData.maybeFor(group.id, user, None, dataService, cacheService)
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
          val fetcher = GithubPublishedBehaviorGroupsFetcher(team, maybeBranch, installedGroupData, githubService, services, ec)
          fetcher.result
        }.getOrElse(Seq())
        val matchResults = FuzzyMatcher[BehaviorGroupData](queryString, installedGroupData ++ publishedGroupData).run
        Ok(Json.toJson(matchResults.map(_.item)).toString)
      }.getOrElse {
        NotFound("")
      }
    }
  }

  def possibleCitiesFor(searchQuery: String) = silhouette.SecuredAction { implicit request =>
    val matches = citiesToTimeZones.possibleCitiesFor(searchQuery)
    Ok(Json.obj("matches" -> matches))
  }

  case class TimeZoneFormInfo(tzName: String, maybeTeamId: Option[String])

  implicit val timeZoneFormInfoReads = Json.reads[TimeZoneFormInfo]

  private val timeZoneForm = Form(
    mapping(
      "tzName" -> nonEmptyText,
      "teamId" -> optional(nonEmptyText)
    )(TimeZoneFormInfo.apply)(TimeZoneFormInfo.unapply)
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
              Ok(Json.toJson(TeamTimeZoneData(
                tz.toString,
                Some(tz.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                OffsetDateTime.now(tz).getOffset.getTotalSeconds
              )).toString)
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
