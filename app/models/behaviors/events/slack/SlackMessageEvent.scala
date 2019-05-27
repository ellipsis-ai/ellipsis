package models.behaviors.events.slack

import akka.actor.ActorSystem
import json.UserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.scheduling.Scheduled
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackMessageEvent(
                              eventContext: SlackEventContext,
                              message: SlackMessage,
                              maybeFile: Option[SlackFile],
                              maybeTs: Option[String],
                              protected val maybeOriginalEventType: Option[EventType],
                              maybeScheduled: Option[Scheduled],
                              override val isUninterruptedConversation: Boolean,
                              override val isEphemeral: Boolean,
                              override val maybeResponseUrl: Option[String],
                              override val beQuiet: Boolean
                            ) extends MessageEvent {

  override type EC = SlackEventContext

  val profile: SlackBotProfile = eventContext.profile
  val channel: String = eventContext.channel

  val eventType: EventType = EventType.chat

  val maybeMessageId: Option[String] = maybeTs

  override def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    maybeTs.map { ts =>
      eventContext.maybePermalinkFor(ts, services)
    }.getOrElse(Future.successful(None))
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType), isUninterruptedConversation = isUninterrupted)
  }

  override def contextualBotPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    if (eventContext.isDirectMessage) {
      Future.successful("")
    } else {
      for {
        botName <- botName(services)
      } yield {
        if (botName == SlackMessageEvent.fallbackBotPrefix) {
          botName
        } else {
          s"@$botName "
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

  lazy val includesBotMention: Boolean = maybeScheduled.isDefined || eventContext.isDirectMessage || profile.includesBotMention(message)

  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = {
    UserData.allFromSlackUserDataListAction(message.userList, ellipsisTeamId, services)
  }

  override val isResponseExpected: Boolean = includesBotMention

  override def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(eventContext.userIdForContext, eventContext.name, maybeChannel, maybeThreadId, ellipsisTeamId).flatMap { maybeConvo =>
      maybeConvo.map(c => Future.successful(Some(c))).getOrElse(maybeConversationRootedHere(dataService))
    }
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(eventContext.userIdForContext, eventContext.name, maybeChannel, maybeTs, ellipsisTeamId)
  }

}

object SlackMessageEvent {

  val fallbackBotPrefix = "..."
  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

}
