package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.team.Team
import play.api.Configuration
import play.api.libs.ws.WSClient
import slack.api.SlackApiClient
import services.{AWSLambdaConstants, DataService, DefaultServices}
import utils.SlackMessageSender

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RunEvent(
                     profile: SlackBotProfile,
                     behavior: Behavior,
                     arguments: Map[String, String],
                     channel: String,
                     maybeThreadId: Option[String],
                     user: String,
                     ts: String,
                     client: SlackApiClient
                  ) extends Event with SlackEvent {

  val messageText: String = ""
  val includesBotMention: Boolean = false
  val isResponseExpected: Boolean = false
  val invocationLogText: String = s"Running behavior ${behavior.id}"

  val teamId: String = behavior.team.id
  val userIdForContext: String = user
  val messageRecipientPrefix: String = messageRecipientPrefixFor(channel)

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT
  lazy val isPublicChannel: Boolean = !isDirectMessage(channel) && !isPrivateChannel(channel)

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, context, maybeChannel, maybeThreadId)
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None
                 )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    SlackMessageSender(
      client,
      user,
      unformattedText,
      forcePrivate,
      channel,
      channel,
      maybeThreadId,
      maybeShouldUnfurl,
      maybeConversation,
      maybeActions
    ).send
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             ): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      responses <- maybeBehaviorVersion.map { behaviorVersion =>
        for {
          params <- dataService.behaviorParameters.allFor(behaviorVersion)
          invocationParams <- Future.successful(arguments.flatMap { case(name, value) =>
            params.find(_.name == name).map { param =>
              (AWSLambdaConstants.invocationParamFor(param.rank - 1), value)
            }
          })
          response <- dataService.behaviorResponses.buildFor(
            this,
            behaviorVersion,
            invocationParams,
            None,
            None,
            services
          )
        } yield Seq(response)
      }.getOrElse(Future.successful(Seq()))
    } yield responses
  }

}
