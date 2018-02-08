package models.loggedevent

import java.time.OffsetDateTime

import json.{BehaviorTriggerData, ScheduledActionData}
import models.IDs
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.behaviors.scheduling.Scheduled
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.libs.json.{JsValue, Json}

case class MessageContext(medium: String, channel: String)


case class LoggedEventContext(maybeUser: Option[User], maybeMessageContext: Option[MessageContext] = None) {
  val maybeUserId: Option[String] = maybeUser.map(_.id)
  val maybeMedium: Option[String] = maybeMessageContext.map(_.medium)
  val maybeChannel: Option[String] = maybeMessageContext.map(_.channel)
}

case class LoggedEvent(
                        id: String,
                        causeType: CauseType,
                        causeDetails: CauseDetails,
                        resultType: ResultType,
                        resultDetails: ResultDetails,
                        maybeUserId: Option[String],
                        createdAt: OffsetDateTime
                      )

case class MessageSentDetails(text: String)

case class TriggerMatchedDetails(messageText: String, trigger: BehaviorTriggerData)

case class ScheduledRunDetails(scheduled: ScheduledActionData)

object DetailsFormatting {

  import json.Formatting._

  lazy implicit val messageSentDetailsFormat = Json.format[MessageSentDetails]
  lazy implicit val triggerMatchedDetailsFormat = Json.format[TriggerMatchedDetails]
  lazy implicit val scheduledRunDetailsFormat = Json.format[ScheduledRunDetails]
}

object LoggedEvent {

  /*

  import DetailsFormatting._

  def newFor(eventType: CauseType, context: LoggedEventContext, details: JsValue) = {
    LoggedEvent(
      IDs.next,
      eventType,
      context.maybeUserId,
      context.maybeMedium,
      context.maybeChannel,
      details,
      OffsetDateTime.now
    )
  }

  def forBotMessageSent(message: String, context: LoggedEventContext): LoggedEvent = newFor(
    BotMessageSent,
    context,
    Json.toJson(MessageSentDetails(message))
  )

  def forTriggerMatched(trigger: MessageTrigger, event: MessageEvent, user: User): LoggedEvent = {
    val context = LoggedEventContext(Some(user), event.maybeChannel.map(ch => MessageContext(event.context, ch)))
    newFor(
      TriggerMatched,
      context,
      Json.toJson(TriggerMatchedDetails(event.messageText, BehaviorTriggerData.buildFor(trigger)))
    )
  }

  def forScheduledRun(scheduled: Scheduled): LoggedEvent = {
    val context =
      LoggedEventContext(
        scheduled.maybeUser,
        scheduled.maybeChannel.map { channel =>
          MessageContext(Conversation.SLACK_CONTEXT, channel)
        })
    newFor(
      ScheduledRun,
      context,
      Json.toJson(ScheduledRunDetails(ScheduledActionData.fromScheduled(scheduled)))
    )
  }

  */

//  def runFromHelp(behaviorVersion: BehaviorVersion, context: LoggedEventContext): Future[Unit]
//
//
//  // Developer events:
//
//  def behaviorGroupVersionSaved(behaviorGroupVersion: BehaviorGroupVersion): Future[Unit]
//
//  def scheduleChanged(scheduled: Scheduled): Future[Unit]
}
