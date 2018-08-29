package models.behaviors.behaviorversion

import utils.Enum

object BehaviorResponseType extends Enum[BehaviorResponseType] {
  val values = List(Normal, Private, Threaded)
  def definitelyFind(name: String): BehaviorResponseType = find(name).getOrElse(Normal)
  def definitelyFind(maybeName: Option[String]): BehaviorResponseType = maybeName.map(definitelyFind).getOrElse(Normal)
}

sealed trait BehaviorResponseType extends BehaviorResponseType.Value {
  val displayName: String

  def channelToUseFor(
                     maybeChannelToForce: Option[String],
                     originatingChannel: String,
                     channelToUse: String,
                     maybeThreadTs: Option[String]
                     ): String = {
    maybeChannelToForce.getOrElse {
      channelToUseFor(originatingChannel, channelToUse, maybeThreadTs)
    }
  }

  def channelToUseFor(
                       originatingChannel: String,
                       channelToUse: String,
                       maybeThreadTs: Option[String]
                     ): String = {
    maybeThreadTs.map(_ => originatingChannel).getOrElse(channelToUse)
  }

  def maybeThreadTsToUseFor(
                             channel: String,
                             originatingChannel: String,
                             maybeThreadTs: Option[String]
                     ): Option[String] = {
    maybeThreadTs
  }
}

case object Normal extends BehaviorResponseType {
  val displayName = "Respond normally"
}

case object Private extends BehaviorResponseType {
  val displayName = "Respond privately"

  override def channelToUseFor(
                               originatingChannel: String,
                               channelToUse: String,
                               maybeThreadTs: Option[String]
                             ): String = channelToUse

  override def maybeThreadTsToUseFor(
                                     channel: String,
                                     originatingChannel: String,
                                     maybeThreadTs: Option[String]
                                   ): Option[String] = {
    if (channel == originatingChannel) {
      maybeThreadTs
    } else {
      None
    }
  }
}

case object Threaded extends BehaviorResponseType {
  val displayName = "Respond in a new thread"
}
