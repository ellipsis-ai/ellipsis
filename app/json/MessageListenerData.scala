package json

import java.time.OffsetDateTime

import models.accounts.{BotContext, SlackContext}
import models.behaviors.messagelistener.MessageListener
import models.team.Team
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import services.DefaultServices
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

case class MessageListenerData(
                                id: String,
                                action: Option[BehaviorVersionData],
                                arguments: JsValue,
                                medium: String,
                                channel: String,
                                channelName: Option[String],
                                maybeThreadId: Option[String],
                                user: UserData,
                                isForCopilot: Boolean,
                                createdAt: OffsetDateTime
                              )

object MessageListenerData {

  def from(listener: MessageListener, services: DefaultServices)(implicit ec: ExecutionContext): Future[MessageListenerData] = {
    val dataService = services.dataService
    val user = listener.user
    val team = listener.behavior.team
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(listener.behavior)
      maybeBehaviorVersionData <- BehaviorVersionData.maybeFor(listener.behavior.id,
        user,
        dataService,
        maybeBehaviorVersion.map(_.groupVersion),
        None)
      userData <- dataService.users.userDataFor(user, team)
      maybeSlackBotProfile <- BotContext.maybeContextFor(listener.medium).map {
        case SlackContext => dataService.slackBotProfiles.maybeFirstFor(team, user)
          // Todo: support for MS Teams
        case _ => {
          Logger.error(s"Creating message listener data isn't supported for ${listener.medium}")
          Future.successful(None)
        }
      }.getOrElse(Future.successful(None))
      maybeChannelData <- maybeSlackBotProfile.map { profile =>
        SlackChannels(services.slackEventService.clientFor(profile)).getInfoFor(listener.channel)
      }.getOrElse(Future.successful(None))
    } yield {
      MessageListenerData(
        listener.id,
        maybeBehaviorVersionData,
        Json.toJson(listener.arguments),
        listener.medium,
        listener.channel,
        maybeChannelData.flatMap(_.name),
        listener.maybeThreadId,
        userData,
        listener.isForCopilot,
        listener.createdAt
      )
    }
  }

}
