package models.behaviors.events

import models.behaviors.conversations.conversation.Conversation

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledMessageEvent(underlying: MessageEvent) extends MessageEvent {

  def eventualMaybeDMChannel: Future[Option[String]] = underlying.eventualMaybeDMChannel

  def isDirectMessage(channel: String) = underlying.isDirectMessage(channel)

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions]
                 )(implicit ec: ExecutionContext) = underlying.sendMessage(text, forcePrivate, maybeShouldUnfurl, maybeConversation, maybeActions)

  lazy val maybeChannel: Option[String] = underlying.maybeChannel
  lazy val teamId: String = underlying.teamId
  lazy val name: String = underlying.name
  lazy val includesBotMention: Boolean = underlying.includesBotMention
  lazy val fullMessageText: String = underlying.fullMessageText
  lazy val isResponseExpected: Boolean = underlying.isResponseExpected
  lazy val userIdForContext: String = underlying.userIdForContext

}
