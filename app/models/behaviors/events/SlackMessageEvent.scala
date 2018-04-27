package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.{ActionChoice, DeveloperContext}
import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import services.caching.CacheService
import services.{AWSLambdaService, DataService, DefaultServices}
import slack.api.SlackApiClient
import utils.{SlackMessageSender, UploadFileSpec}

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
                              client: SlackApiClient,
                              maybeOriginalEventType: Option[EventType],
                              override val isUninterruptedConversation: Boolean
                            ) extends MessageEvent with SlackEvent {

  val eventType: EventType = EventType.chat

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType), isUninterruptedConversation = isUninterrupted)
  }

  lazy val isBotMessage: Boolean = profile.userId == user

  override def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    services.dataService.slackBotProfiles.maybeNameFor(profile).map { maybeName =>
      maybeName.getOrElse(SlackMessageEvent.fallbackBotPrefix)
    }
  }

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

  override val isResponseExpected: Boolean = includesBotMention
  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, maybeThreadId, teamId).flatMap { maybeConvo =>
      maybeConvo.map(c => Future.successful(Some(c))).getOrElse(maybeConversationRootedHere(dataService))
    }
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, Some(ts), teamId)
  }

  def channelForSend(
                      forcePrivate: Boolean,
                      maybeConversation: Option[Conversation],
                      cacheService: CacheService
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    (if (forcePrivate) {
      eventualMaybeDMChannel(cacheService)
    } else {
      Future.successful(maybeConversation.flatMap(_.maybeChannel))
    }).map { maybeChannel =>
      maybeChannel.getOrElse(channel)
    }
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
      channelToUse <- channelForSend(forcePrivate, maybeConversation, services.cacheService)
      botName <- botName(services)
      maybeTs <- SlackMessageSender(
        client,
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
        message.userList
      ).send
    } yield maybeTs
  }

}

object SlackMessageEvent {

  val fallbackBotPrefix = "..."
  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

}
