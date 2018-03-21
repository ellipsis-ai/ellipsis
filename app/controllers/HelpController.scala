package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class HelpController @Inject() (
                                   val configuration: Configuration,
                                   val dataService: DataService,
                                   val silhouette: Silhouette[EllipsisEnv],
                                   val assetsProvider: Provider[RemoteAssets],
                                   implicit val ec: ExecutionContext
                                 ) extends EllipsisController {

  private def maybeOverrideBotName(slackTeamId: String, botName: String): Future[Option[String]] = {
    for {
      maybeName <- dataService.slackBotProfiles.maybeNameFor(slackTeamId)
    } yield {
      maybeName.filter(_ == botName)
    }
  }

  private def botNameFor(maybeTeamAccess: Option[UserTeamAccess], maybeSlackTeamId: Option[String], maybeBotName: Option[String]): Future[String] = {
    val maybeLoggedInBotName = maybeTeamAccess.flatMap(_.maybeBotName)
    maybeLoggedInBotName.map { name =>
      Future.successful(name)
    }.getOrElse {
      for {
        maybeName <- (for {
          slackTeamId <- maybeSlackTeamId
          botName <- maybeBotName
        } yield {
          maybeOverrideBotName(slackTeamId, botName)
        }).getOrElse(Future.successful(None))
      } yield {
        maybeName.getOrElse(Team.defaultBotName)
      }
    }
  }

  def devMode(maybeSlackTeamId: Option[String], maybeBotName: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    for {
      maybeTeamAccess <- maybeTeamAccessFor(request, dataService)
      botName <- botNameFor(maybeTeamAccess, maybeSlackTeamId, maybeBotName)
    } yield {
      Ok(views.html.help.devMode("Dev mode", viewConfig(maybeTeamAccess), botName))
    }
  }

  def scheduledMessages(maybeSlackTeamId: Option[String], maybeBotName: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    for {
      maybeTeamAccess <- maybeTeamAccessFor(request, dataService)
      botName <- botNameFor(maybeTeamAccess, maybeSlackTeamId, maybeBotName)
    } yield {
      Ok(views.html.help.scheduledMessages("Scheduling actions", viewConfig(maybeTeamAccess), botName))
    }
  }

}
