package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import models.behaviors._
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.behaviors.triggers.messagetrigger.MessageTrigger
import services.DefaultServices
import services.DataService
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Conversation {
  val id: String
  val behaviorVersion: BehaviorVersion
  val maybeTrigger: Option[MessageTrigger]
  val maybeTriggerMessage: Option[String]
  val conversationType: String
  val context: String
  val maybeChannel: Option[String]
  val maybeThreadId: Option[String]
  val userIdForContext: String
  val startedAt: OffsetDateTime
  val maybeLastInteractionAt: Option[OffsetDateTime]
  val state: String
  val maybeScheduledMessageId: Option[String]
  val isScheduled: Boolean = maybeScheduledMessageId.isDefined

  def isPending: Boolean = state == Conversation.PENDING_STATE

  def staleCutoff: OffsetDateTime = OffsetDateTime.now.minusHours(1)

  def pendingEventKey: String = s"pending-event-for-$id"

  def isStale: Boolean = {
    maybeThreadId.isEmpty && maybeLastInteractionAt.getOrElse(startedAt).isBefore(staleCutoff)
  }

  def shouldBeBackgrounded: Boolean = {
    startedAt.plusSeconds(Conversation.SECONDS_UNTIL_BACKGROUNDED).isBefore(OffsetDateTime.now)
  }

  private def maybeSlackPlaceholderEventAction(services: DefaultServices): DBIO[Option[Event]] = {
    services.dataService.slackBotProfiles.allForAction(behaviorVersion.team).map { botProfiles =>
      for {
        botProfile <- botProfiles.headOption
        channel <- maybeChannel
      // TODO: Create a new class of synthetic events that doesn't need a SlackUserInfo list
      // https://github.com/ellipsis-ai/ellipsis/issues/1719
      // For now, there's no text in the event, so the empty user list doesn't matter
      } yield SlackMessageEvent(botProfile, channel, None, userIdForContext, SlackMessage.blank, SlackTimestamp.now, services.slackEventService.clientFor(botProfile))
    }
  }

  def maybePlaceholderEventAction(services: DefaultServices): DBIO[Option[Event]] = {
    context match {
      case Conversation.SLACK_CONTEXT => maybeSlackPlaceholderEventAction(services)
      case _ => DBIO.successful(None)
    }
  }

  def copyWithMaybeThreadId(maybeId: Option[String]): Conversation

  def copyWithLastInteractionAt(dt: OffsetDateTime): Conversation

  val stateRequiresPrivateMessage: Boolean = false

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation]
  def cancel(dataService: DataService): Future[Conversation] = updateStateTo(Conversation.DONE_STATE, dataService)
  def updateWith(event: Event, services: DefaultServices): Future[Conversation]

  def respondAction(
                     event: Event,
                     isReminding: Boolean,
                     services: DefaultServices
                   ): DBIO[BotResult]

  def respond(
               event: Event,
               isReminding: Boolean,
               services: DefaultServices
             ): Future[BotResult]

  def resultFor(
                 event: Event,
                 services: DefaultServices
               ): Future[BotResult] = {
    for {
      updatedConversation <- updateWith(event, services)
      result <- updatedConversation.respond(event, isReminding=false, services)
    } yield result
  }

  def maybeNextParamToCollect(
                               event: Event,
                               services: DefaultServices
                             ): Future[Option[BehaviorParameter]]

  def maybeRemindResultAction(
                               services: DefaultServices
                            ): DBIO[Option[BotResult]] = {
    maybePlaceholderEventAction(services).flatMap { maybeEvent =>
      maybeEvent.map { event =>
        respondAction(event, isReminding=true, services).map { result =>
          val intro = s"Hey <@$userIdForContext>, don’t forget, I’m still waiting for your answer to this:"
          val actions = Seq(SlackMessageActionButton(STOP_CONVERSATION, "Stop asking", id))
          val question = result.text
          val attachment = SlackMessageActions(STOP_CONVERSATION, actions, Some(question), None)
          Some(TextWithActionsResult(result.event, Some(this), intro, result.forcePrivateResponse, attachment))
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
      startedAt,
      maybeLastInteractionAt,
      state,
      maybeScheduledMessageId
    )
  }
}

object Conversation {
  val NEW_STATE = "new"
  val PENDING_STATE = "pending"
  val DONE_STATE: String = "done"

  val SLACK_CONTEXT = "slack"
  val API_CONTEXT = "api"

  val LEARN_BEHAVIOR = "learn_behavior"
  val INVOKE_BEHAVIOR = "invoke_behavior"

  val SECONDS_UNTIL_BACKGROUNDED = 3600
}
