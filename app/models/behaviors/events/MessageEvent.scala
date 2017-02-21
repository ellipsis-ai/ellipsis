package models.behaviors.events

import models.behaviors.conversations.conversation.Conversation
import services.DataService

import scala.concurrent.Future
import scala.util.matching.Regex

trait MessageEvent extends Event {

  lazy val invocationLogText: String = relevantMessageText

  def isDirectMessage(channel: String): Boolean

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, context, maybeChannel, maybeThreadId, maybeChannel.exists(isDirectMessage))
  }

}

object MessageEvent {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}
