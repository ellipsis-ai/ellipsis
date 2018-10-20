package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BehaviorResponse, DeveloperContext}
import models.team.Team
import play.api.Configuration
import services.{DataService, DefaultServices}
import utils.{SlackMessageSender, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}

case class SlashCommandEvent(
                              profile: SlackBotProfile,
                              channel: String,
                              user: String,
                              message: SlackMessage,
                              responseUrl: String
                            ) extends Event with SlackEvent {

  val eventType: EventType = EventType.chat

  override val isEphemeral: Boolean = true
  override val maybeResponseUrl: Option[String] = Some(responseUrl)

  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  lazy val messageText: String = message.originalText
  lazy val invocationLogText: String = relevantMessageText

  val maybeThreadId: Option[String] = None
  val maybeOriginalEventType: Option[EventType] = None

  override val isResponseExpected: Boolean = true
  val includesBotMention: Boolean = true

  override val beQuiet: Boolean = true

  def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    Future.successful(None)
  }

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    Future.successful(Seq())
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(this, maybeTeam, maybeChannel, context, maybeLimitToBehavior)
      activatedTriggers <- activatedTriggersIn(possibleActivatedTriggers, dataService)
      responses <- Future.sequence(activatedTriggers.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <- dataService.behaviorResponses.buildFor(
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
            None,
            None,
            userExpectsResponse = true
          )
        } yield response
      })
    } yield responses
  }


  def messageUserDataList: Set[MessageUserData] = {
    message.userList.map(MessageUserData.fromSlackUserData)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

  def sendMessage(
                   unformattedText: String,
                   responseType: BehaviorResponseType,
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
      maybeDMChannel <- eventualMaybeDMChannel(services)
      botName <- botName(services)
      maybeTs <- SlackMessageSender(
        services.slackApiService.clientFor(profile),
        user,
        profile.slackTeamId,
        unformattedText,
        responseType,
        developerContext,
        channel,
        maybeDMChannel,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachmentGroups,
        files,
        choices,
        configuration,
        botName,
        messageUserDataList(maybeConversation, services),
        services,
        isEphemeral,
        maybeResponseUrl,
        beQuiet
      ).send
    } yield maybeTs
  }

}
