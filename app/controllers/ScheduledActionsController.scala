package controllers

import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ScheduledActionsController @Inject()(
                                            val messagesApi: MessagesApi,
                                            val configuration: Configuration,
                                            val silhouette: Silhouette[EllipsisEnv],
                                            val dataService: DataService,
                                            implicit val actorSystem: ActorSystem
                                          ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity

    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          result <- teamAccess.maybeTargetTeam.map { team =>
            for {
              scheduledMessages <- dataService.scheduledMessages.allForTeam(team)
              scheduledBehaviors <- dataService.scheduledBehaviors.allForTeam(team)
              maybeBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
              channelList <- maybeBotProfile.map { botProfile =>
                dataService.slackBotProfiles.channelsFor(botProfile).listInfos
              }.getOrElse(Future.successful(Seq()))
              scheduledMessageData <- ScheduledActionData.fromScheduledMessages(scheduledMessages, channelList)
              scheduledBehaviorData <- ScheduledActionData.fromScheduledBehaviors(scheduledBehaviors, dataService, channelList)
              maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
              maybeSlackProfile <- maybeSlackLinkedAccount.map { linkedAccount =>
                dataService.slackProfiles.find(linkedAccount.loginInfo)
              }.getOrElse(Future.successful(None))
            } yield {
              val pageData = ScheduledActionsConfig(
                containerId = "scheduling",
                teamId = team.id,
                scheduledActions = scheduledMessageData ++ scheduledBehaviorData,
                channelList = ScheduleChannelData.fromChannelLikeList(channelList),
                teamTimeZone = team.maybeTimeZone.map(_.toString),
                teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                slackUserId = maybeSlackProfile.map(_.loginInfo.providerKey)
              )
              Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/scheduling/index", Json.toJson(pageData)))
            }
          }.getOrElse {
            Future.successful(NotFound("Team not found"))
          }
        } yield result
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            Ok(views.html.scheduledactions.index(viewConfig(Some(teamAccess)), maybeTeamId))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }
}
