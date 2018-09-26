package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BotResult, DeveloperContext}
import play.api.Configuration
import services.{DataService, DefaultServices}
import utils.{SlackMessageReactionHandler, SlackMessageSender, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackMessageEvent(
                              profile: SlackBotProfile,
                              userSlackTeamId: String,
                              channel: String,
                              maybeThreadId: Option[String],
                              user: String,
                              message: SlackMessage,
                              maybeFile: Option[SlackFile],
                              ts: String,
                              maybeOriginalEventType: Option[EventType],
                              override val isUninterruptedConversation: Boolean,
                              override val isEphemeral: Boolean,
                              override val maybeResponseUrl: Option[String]
                            ) extends MessageEvent with SlackEvent {

  val eventType: EventType = EventType.chat

  override def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val client = services.slackApiService.clientFor(profile)
    client.permalinkFor(channel, ts)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType), isUninterruptedConversation = isUninterrupted)
  }

  override val isBotMessage: Boolean = profile.userId == user

  override def contextualBotPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    if (isDirectMessage) {
      Future.successful("")
    } else {
      for {
        name <- botName(services)
      } yield {
        if (name == SlackMessageEvent.fallbackBotPrefix) {
          name
        } else {
          s"@$name "
        }
      }
    }
  }

  val messageText: String = message.originalText

  override val relevantMessageTextWithFormatting: String = {
    message.withoutBotPrefix
  }

  override val relevantMessageText: String = {
    message.unformattedText
  }

  lazy val includesBotMention: Boolean = isDirectMessage || profile.includesBotMention(message)

  def messageUserDataList: Set[MessageUserData] = {
    message.userList.map(MessageUserData.fromSlackUserData)
  }

  override val isResponseExpected: Boolean = includesBotMention
  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  override def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, maybeThreadId, teamId).flatMap { maybeConvo =>
      maybeConvo.map(c => Future.successful(Some(c))).getOrElse(maybeConversationRootedHere(dataService))
    }
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, Some(ts), teamId)
  }

  override def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                                    (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    SlackMessageReactionHandler.handle(services.slackApiService.clientFor(profile), eventualResults, channel, ts)
    eventualResults
  }

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
        maybeResponseUrl
      ).send
    } yield maybeTs
  }

}

object SlackMessageEvent {

  val fallbackBotPrefix = "..."
  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

}
