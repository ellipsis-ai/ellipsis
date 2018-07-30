package controllers.admin


import javax.inject.Inject
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import json.AdminTeamData
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class TeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AdminAuth {

  private def adminTeamDataFor(team: Team): Future[AdminTeamData] = {
    for {
      maybeSlackBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
      maybeLastInvocationDate <- dataService.invocationLogEntries.lastInvocationDateForTeam(team)
    } yield {
      AdminTeamData(
        team.id,
        team.name,
        team.timeZone.toString,
        team.createdAt,
        maybeSlackBotProfile.exists(_.allowShortcutMention),
        maybeLastInvocationDate
      )
    }
  }

  def list(page: Int, perPage: Int, maybeUpdatedTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      if (page < 0 || perPage < 0) {
        Future {
          BadRequest("page and perPage parameters cannot be less than zero!")
        }
      } else {
        for {
          count <- dataService.teams.allCount
          pageData <- getPageData(count, page, perPage)
          teams <- dataService.teams.allTeamsPaged(pageData.current, pageData.size)
          adminTeamsData <- Future.sequence(teams.map(adminTeamDataFor)).map(_.sorted.reverse)
        } yield {
          Ok(views.html.admin.teams.list(
            viewConfig(None),
            adminTeamsData,
            count,
            pageData.current,
            pageData.size,
            pageData.total,
            maybeUpdatedTeamId
          ))
        }
      }
    })
  }


  private case class PageData(current: Int, size: Int, total: Int)

  private def getPageData(count: Int, page: Int, perPage: Int): Future[PageData] = {
    var realPage = page
    var realPerPage = perPage
    var lastPage = (math.ceil(count/realPerPage)).toInt

    // if page is zero and there are less than 50 teams display them all.
    if (page == 0 && count < 50) {
      realPage = 1
      realPerPage = count
      lastPage = 1
    }

    if ( count % realPerPage > 0) lastPage = +1

    Future { new PageData(realPage, realPerPage, lastPage) }
  }

  case class ToggleSlackBotShortcutInfo(teamId: String, enableShortcut: Boolean)

  private val toggleSlackBotShortcutForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "enableShortcut" -> boolean
    )(ToggleSlackBotShortcutInfo.apply)(ToggleSlackBotShortcutInfo.unapply)
  )
  def toggleBotShortcutForTeam() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      toggleSlackBotShortcutForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          for {
            maybeTeam <- dataService.teams.find(info.teamId)
            maybeSlackBotProfile <- maybeTeam.map { team =>
              dataService.slackBotProfiles.allFor(team).map(_.headOption)
            }.getOrElse(Future.successful(None))
            maybeEnabled <- maybeSlackBotProfile.map { botProfile =>
              dataService.slackBotProfiles.toggleMentionShortcut(botProfile, info.enableShortcut)
            }.getOrElse(Future.successful(None))
          } yield {
            maybeEnabled.map { _ =>
              Redirect(routes.TeamsController.list(0, 10, Some(info.teamId)))
            }.getOrElse {
              NotFound(s"Unable to modify shortcut setting for team ${info.teamId}")
            }
          }
        }
      )
    })
  }
}


