package models.behaviors.behaviorversion

import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import utils.Enum

object BehaviorResponseType extends Enum[BehaviorResponseType] {
  val values = List(Normal, Private, Threaded)
  def definitelyFind(name: String): BehaviorResponseType = find(name).getOrElse(Normal)
  def definitelyFind(maybeName: Option[String]): BehaviorResponseType = maybeName.map(definitelyFind).getOrElse(Normal)
}

sealed trait BehaviorResponseType extends BehaviorResponseType.Value {
  val id: String = toString
  val displayName: String

  def channelToUseFor(
                       originatingChannel: String,
                       maybeConversation: Option[Conversation],
                       maybeThreadTs: Option[String],
                       maybeDMChannel: Option[String]
                     ): String = {
    maybeThreadTs.map(_ => originatingChannel).orElse {
      maybeConversation.flatMap(_.maybeChannel)
    }.getOrElse(originatingChannel)
  }

  def maybeThreadTsToUseFor(
                             channelToUse: String,
                             originatingChannel: String,
                             maybeConversation: Option[Conversation],
                             maybeThreadTs: Option[String],
                             maybeTriggeringMessageId: Option[String]
                     ): Option[String] = {
    maybeConversation.flatMap(_.maybeThreadId).orElse(maybeThreadTs)
  }

  def maybeOriginalMessageThreadIdFor(event: Event): Option[String] = None
}

case object Normal extends BehaviorResponseType {
  val displayName = "Respond normally"
}

case object Private extends BehaviorResponseType {
  val displayName = "Respond privately"

  override def channelToUseFor(
                                originatingChannel: String,
                                maybeConversation: Option[Conversation],
                                maybeThreadTs: Option[String],
                                maybeDMChannel: Option[String]
                              ): String = {
    maybeDMChannel.getOrElse(super.channelToUseFor(originatingChannel, maybeConversation, maybeThreadTs, maybeDMChannel))
  }

  override def maybeThreadTsToUseFor(
                                      channelToUse: String,
                                      originatingChannel: String,
                                      maybeConversation: Option[Conversation],
                                      maybeThreadTs: Option[String],
                                      maybeTriggeringMessageId: Option[String]
                                   ): Option[String] = {
    if (channelToUse == originatingChannel) {
      super.maybeThreadTsToUseFor(channelToUse, originatingChannel, maybeConversation, maybeThreadTs, maybeTriggeringMessageId)
    } else {
      None
    }
  }
}

case object Threaded extends BehaviorResponseType {
  val displayName = "Respond in a new thread"

  override def maybeOriginalMessageThreadIdFor(event: Event): Option[String] = event.maybeMessageId

  override def maybeThreadTsToUseFor(
                                      channelToUse: String,
                                      originatingChannel: String,
                                      maybeConversation: Option[Conversation],
                                      maybeThreadTs: Option[String],
                                      maybeTriggeringMessageId: Option[String]
                                    ): Option[String] = {
    super.maybeThreadTsToUseFor(channelToUse, originatingChannel, maybeConversation, maybeThreadTs, maybeTriggeringMessageId).orElse(maybeTriggeringMessageId)
  }
}
