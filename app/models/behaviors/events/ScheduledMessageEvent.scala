package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduledmessage.ScheduledMessage
import services.DataService

import scala.concurrent.Future

case class ScheduledMessageEvent(underlying: MessageEvent, scheduledMessage: ScheduledMessage) extends MessageEvent {

  def eventualMaybeDMChannel(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    underlying.eventualMaybeDMChannel(dataService)
  }

  def isDirectMessage(channel: String) = underlying.isDirectMessage(channel)

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions],
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem) = {
    underlying.sendMessage(text, forcePrivate, maybeShouldUnfurl, maybeConversation, maybeActions, dataService)
  }

  lazy val maybeChannel: Option[String] = underlying.maybeChannel
  lazy val maybeThreadId: Option[String] = underlying.maybeThreadId
  lazy val teamId: String = underlying.teamId
  lazy val name: String = underlying.name
  lazy val includesBotMention: Boolean = underlying.includesBotMention
  lazy val messageText: String = underlying.messageText
  lazy val isResponseExpected: Boolean = underlying.isResponseExpected
  lazy val userIdForContext: String = underlying.userIdForContext
  override val maybeScheduledMessage: Option[ScheduledMessage] = Some(scheduledMessage)

}
