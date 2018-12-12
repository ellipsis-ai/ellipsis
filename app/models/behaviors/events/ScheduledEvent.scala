package models.behaviors.events

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.slack.{SlackMessageEvent, SlackRunEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.testing.TestMessageEvent
import models.behaviors.{ActionChoice, DeveloperContext}
import models.team.Team
import play.api.libs.json.JsObject
import services.DefaultServices
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

sealed trait ScheduledEvent extends Event {

  type UE <: Event
  type S <: Scheduled

  val underlying: UE
  val scheduled: S

  val eventType: EventType = EventType.scheduled
  val maybeOriginalEventType: Option[EventType] = None
  def withOriginalEventType(originalEventType: EventType, isUninterruptedConversation: Boolean): Event = this

  override def sendMessage(
                            text: String,
                            responseType: BehaviorResponseType,
                            maybeShouldUnfurl: Option[Boolean],
                            maybeConversation: Option[Conversation],
                            attachments: Seq[MessageAttachment],
                            files: Seq[UploadFileSpec],
                            choices: Seq[ActionChoice],
                            developerContext: DeveloperContext,
                            services: DefaultServices
                          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    underlying.sendMessage(text, responseType, maybeShouldUnfurl, maybeConversation, attachments, files, choices, developerContext, services)
  }

  override def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    underlying.detailsFor(services)
  }

  override val ellipsisTeamId: String = underlying.ellipsisTeamId
  lazy val includesBotMention: Boolean = underlying.includesBotMention
  lazy val messageText: String = underlying.messageText
  lazy val invocationLogText: String = underlying.invocationLogText
  lazy val isResponseExpected: Boolean = underlying.isResponseExpected
  override val maybeScheduled: Option[Scheduled] = Some(scheduled)
  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = underlying.messageUserDataListAction(services)

  val maybeMessageIdForReaction: Option[String] = None

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext) = underlying.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, services)

}

case class ScheduledBehaviorSlackEvent(underlying: SlackRunEvent, scheduled: ScheduledBehavior) extends ScheduledEvent {

  override type UE = SlackRunEvent
  override type S = ScheduledBehavior
  override type EC = SlackEventContext

  lazy val eventContext: SlackEventContext = underlying.eventContext
}

case class ScheduledMessageSlackEvent(underlying: SlackMessageEvent, scheduled: ScheduledMessage) extends ScheduledEvent {

  override type UE = SlackMessageEvent
  override type S = ScheduledMessage
  override type EC = SlackEventContext

  lazy val eventContext: SlackEventContext = underlying.eventContext
}

case class ScheduledMessageTestEvent(underlying: TestMessageEvent, scheduled: ScheduledMessage) extends ScheduledEvent {

  override type UE = TestMessageEvent
  override type S = ScheduledMessage
  override type EC = TestEventContext

  lazy val eventContext: TestEventContext = underlying.eventContext
}

