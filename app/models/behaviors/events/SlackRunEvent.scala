package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class SlackRunEvent(
                           eventContext: SlackEventContext,
                           behaviorVersion: BehaviorVersion,
                           arguments: Map[String, String],
                           maybeOriginalEventType: Option[EventType],
                           override val isEphemeral: Boolean,
                           override val maybeResponseUrl: Option[String],
                           maybeTriggeringMessageTs: Option[String]
                        ) extends RunEvent {

  override type EC = SlackEventContext

  val eventType: EventType = EventType.api

  val maybeMessageIdForReaction: Option[String] = maybeTriggeringMessageTs

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

  override def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                                    (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventContext.reactionHandler(eventualResults, maybeTriggeringMessageTs, services)
  }

}
