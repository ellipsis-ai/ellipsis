package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.accounts.{MSAzureActiveDirectoryContext, MSTeamsContext, SlackContext}
import models.behaviors._
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events._
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events.slack._
import models.behaviors.triggers.Trigger
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.{ExecutionContext, Future}

trait Conversation {
  val id: String
  val behaviorVersion: BehaviorVersion
  val maybeTrigger: Option[Trigger]
  val maybeTriggerMessage: Option[String]
  val maybeOriginalEventType: Option[EventType]
  val maybeParentId: Option[String]
  val conversationType: String
  val context: String
  val maybeChannel: Option[String]
  val maybeThreadId: Option[String]
  val userIdForContext: String
  val maybeTeamIdForContext: Option[String]
  val startedAt: OffsetDateTime
  val maybeLastInteractionAt: Option[OffsetDateTime]
  val state: String
  val maybeScheduledMessageId: Option[String]
  val isScheduled: Boolean = maybeScheduledMessageId.isDefined

  def isPending: Boolean = state == Conversation.PENDING_STATE
  def isDone: Boolean = state == Conversation.DONE_STATE

  def staleCutoff: OffsetDateTime = OffsetDateTime.now.minusHours(1)

  def pendingEventKey: String = s"pending-event-for-$id"

  def isStale: Boolean = {
    maybeThreadId.isEmpty && maybeLastInteractionAt.getOrElse(startedAt).isBefore(staleCutoff)
  }

  def shouldBeBackgrounded: Boolean = {
    startedAt.plusSeconds(Conversation.SECONDS_UNTIL_BACKGROUNDED).isBefore(OffsetDateTime.now)
  }

  private def maybeSlackPlaceholderEventAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Event]] = {
    services.dataService.slackBotProfiles.allForAction(behaviorVersion.team).map { botProfiles =>
      for {
        botProfile <- botProfiles.headOption
        channel <- maybeChannel
      // TODO: Create a new class for placeholder events
      // https://github.com/ellipsis-ai/ellipsis/issues/1719
      } yield SlackMessageEvent(
        SlackEventContext(
          botProfile,
          channel,
          None,
          userIdForContext
        ),
        SlackMessage.blank,
        None,
        None,
        None, // TODO: Pass the original event type down to here if we actually care about it, but it doesn't seem useful at present
        isUninterruptedConversation = false,
        isEphemeral = false,
        None,
        beQuiet = false
      )
    }
  }

  def maybePlaceholderEventAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Event]] = {
    context match {
      case Conversation.SLACK_CONTEXT => maybeSlackPlaceholderEventAction(services)
      case _ => DBIO.successful(None) // TODO: MS Teams
    }
  }

  def copyWithMaybeThreadId(maybeId: Option[String]): Conversation

  def copyWithLastInteractionAt(dt: OffsetDateTime): Conversation

  val stateRequiresPrivateMessage: Boolean = false

  def updateStateToAction(newState: String, dataService: DataService): DBIO[Conversation]
  def cancelAction(dataService: DataService): DBIO[Conversation] = updateStateToAction(Conversation.DONE_STATE, dataService)
  def updateWithAction(event: Event, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation]

  def respondAction(
                     event: Event,
                     isReminding: Boolean,
                     services: DefaultServices
                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult]

  def respond(
               event: Event,
               isReminding: Boolean,
               services: DefaultServices
             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult]

  def resultForAction(
                 event: Event,
                 services: DefaultServices
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      updatedConversation <- updateWithAction(event, services)
      result <- updatedConversation.respondAction(event, isReminding=false, services)
    } yield result
  }

  def maybeNextParamToCollect(
                               event: Event,
                               services: DefaultServices
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BehaviorParameter]]

  def maybeRemindResultAction(
                               services: DefaultServices
                            )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[BotResult]] = {
    maybePlaceholderEventAction(services).flatMap { maybeEvent =>
      maybeEvent.map { event =>
        respondAction(event, isReminding=true, services).map { result =>
          val intro = s"Hey <@$userIdForContext>, don’t forget, I’m still waiting for your answer to this:"
          val callbackId = stopConversationCallbackIdFor(event.eventContext.userIdForContext, Some(id))
          val eventContext = event.eventContext
          val actionList = Seq(eventContext.messageActionButtonFor(callbackId, "Stop asking", id))
          val question = result.text
          val attachment = eventContext.messageAttachmentFor(maybeText = Some(question), maybeCallbackId = Some(callbackId), actions = actionList)
          Some(TextWithAttachmentsResult(result.event, Some(this), intro, result.responseType, Seq(attachment)))
        }
      }.getOrElse(DBIO.successful(None))
    }
  }

  def toRaw: RawConversation = {
    RawConversation(
      id,
      behaviorVersion.id,
      maybeTrigger.map(_.id),
      maybeTriggerMessage,
      conversationType,
      context,
      maybeChannel,
      maybeThreadId,
      userIdForContext,
      maybeTeamIdForContext,
      startedAt,
      maybeLastInteractionAt,
      state,
      maybeScheduledMessageId,
      maybeOriginalEventType.map(_.toString),
      maybeParentId
    )
  }
}

object Conversation {
  val NEW_STATE = "new"
  val PENDING_STATE = "pending"
  val DONE_STATE: String = "done"

  val SLACK_CONTEXT = SlackContext.toString
  val MS_TEAMS_CONTEXT = MSTeamsContext.toString
  val MS_AAD_CONTEXT = MSAzureActiveDirectoryContext.toString
  val API_CONTEXT = "api"

  val LEARN_BEHAVIOR = "learn_behavior"
  val INVOKE_BEHAVIOR = "invoke_behavior"

  val SECONDS_UNTIL_BACKGROUNDED = 3600

  val CANCEL_MENU_ITEM_TEXT = "Cancel the current action"
  val START_AGAIN_MENU_ITEM_TEXT = "Try again with different answers…"
}
