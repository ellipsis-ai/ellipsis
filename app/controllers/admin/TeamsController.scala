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
import utils.PageData

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
      botProfiles <- dataService.slackBotProfiles.allFor(team)
      /* TODO: This is super slow, not sure why: */
      maybeLastInvocationDate <- Future.successful(None) /*dataService.invocationLogEntries.lastInvocationDateForTeam(team)*/
    } yield {
      AdminTeamData(
        team.id,
        team.name,
        team.timeZone.toString,
        team.createdAt,
        botProfiles.exists(_.allowShortcutMention),
        maybeLastInvocationDate
      )
    }
  }

  def list(page: Int, perPage: Int, maybeUpdatedTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      if (page < 1 || perPage < 1) {
        Future {
          BadRequest("page and perPage parameters cannot be less than 1!")
        }
      } else {
        for {
          count <- dataService.teams.allCount
          pageData <- Future.successful(PageData.pageDataFor(count, page, perPage))
          teams <- dataService.teams.allTeamsPaged(pageData.currentPage, pageData.pageSize)
          adminTeamsData <- Future.sequence(teams.map(adminTeamDataFor)).map(_.sorted.reverse)
        } yield {
          Ok(views.html.admin.teams.list(
            viewConfig(None),
            teams = adminTeamsData,
            teamCount = count,
            currentPage = pageData.currentPage,
            pageSize = pageData.pageSize,
            totalPages = pageData.totalPages,
            maybeUpdatedTeamId = maybeUpdatedTeamId
          ))
        }
      }
    })
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
            botProfiles <- maybeTeam.map { team =>
              dataService.slackBotProfiles.allFor(team)
            }.getOrElse(Future.successful(Seq()))
            enabledFlags <- Future.sequence(botProfiles.map { botProfile =>
              dataService.slackBotProfiles.toggleMentionShortcut(botProfile, info.enableShortcut)
            })
          } yield {
            if (enabledFlags.contains(None)) {
              NotFound(s"Unable to modify shortcut setting for team ${info.teamId}")
            } else {
              Redirect(routes.TeamsController.list(1, 10, Some(info.teamId)))
            }
          }
        }
      )
    })
  }
}


