package models.behaviors.events.slack

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventType, RunEvent, SlackEventContext}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class SlackRunEvent(
                           eventContext: SlackEventContext,
                           behaviorVersion: BehaviorVersion,
                           arguments: Map[String, String],
                           eventType: EventType,
                           maybeOriginalEventType: Option[EventType],
                           override val isEphemeral: Boolean,
                           override val maybeResponseUrl: Option[String],
                           maybeTriggeringMessageTs: Option[String]
                        ) extends RunEvent {

  override type EC = SlackEventContext

  val maybeMessageId: Option[String] = maybeTriggeringMessageTs

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

  override def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                                    (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventContext.reactionHandler(eventualResults, maybeTriggeringMessageTs, services)
  }

  override def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    maybeTriggeringMessageTs.map { ts =>
      eventContext.maybePermalinkFor(ts, services)
    }.getOrElse(Future.successful(None))
  }

}
