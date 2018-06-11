package models.behaviors.events

import akka.actor.ActorSystem
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BehaviorResponse, DeveloperContext}
import models.team.Team
import play.api.Configuration
import services.{AWSLambdaConstants, DataService, DefaultServices}
import utils.{SlackMessageSender, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}

case class RunEvent(
                     profile: SlackBotProfile,
                     userSlackTeamId: String,
                     behavior: Behavior,
                     arguments: Map[String, String],
                     channel: String,
                     maybeThreadId: Option[String],
                     user: String,
                     ts: String,
                     maybeOriginalEventType: Option[EventType]
                  ) extends Event with SlackEvent {

  val eventType: EventType = EventType.api
  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

  val messageText: String = ""
  val includesBotMention: Boolean = false
  val isResponseExpected: Boolean = false
  val invocationLogText: String = s"Running behavior ${behavior.id}"

  val teamId: String = behavior.team.id
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, context, maybeChannel, maybeThreadId, teamId)
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices,
                   configuration: Configuration
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    for {
      botName <- botName(services)
      channelToUse <- channelForSend(forcePrivate, maybeConversation, services)
      maybeTs <- SlackMessageSender(
        services.slackApiService.clientFor(profile),
        user,
        profile.slackTeamId,
        unformattedText,
        forcePrivate = forcePrivate,
        developerContext,
        channel,
        channelToUse,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachmentGroups,
        files,
        choices,
        configuration,
        botName,
        Set.empty[SlackUserData],
        services
      ).send
    } yield maybeTs
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
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
            None
          )
        } yield Seq(response)
      }.getOrElse(Future.successful(Seq()))
    } yield responses
  }

}
