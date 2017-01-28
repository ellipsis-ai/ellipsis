package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Conversation {
  val id: String
  val trigger: MessageTrigger
  val triggerMessage: String
  val behaviorVersion: BehaviorVersion = trigger.behaviorVersion
  val conversationType: String
  val context: String
  val maybeThreadId: Option[String]
  val userIdForContext: String
  val startedAt: OffsetDateTime
  val state: String

  def isPending: Boolean = state == Conversation.PENDING_STATE

  def shouldBeBackgrounded: Boolean = {
    startedAt.plusSeconds(Conversation.SECONDS_UNTIL_BACKGROUNDED).isBefore(OffsetDateTime.now)
  }

  def maybeChannel: Option[String] = context.split("#").tail.headOption

  def copyWithMaybeThreadId(maybeId: Option[String]): Conversation

  val stateRequiresPrivateMessage: Boolean = false

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation]
  def cancel(dataService: DataService): Future[Conversation] = updateStateTo(Conversation.DONE_STATE, dataService)
  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation]
  def respond(
               event: MessageEvent,
               lambdaService: AWSLambdaService,
               dataService: DataService,
               cache: CacheApi,
               ws: WSClient,
               configuration: Configuration
             ): Future[BotResult]

  def resultFor(
                 event: MessageEvent,
                 lambdaService: AWSLambdaService,
                 dataService: DataService,
                 cache: CacheApi,
                 ws: WSClient,
                 configuration: Configuration
               ): Future[BotResult] = {
    for {
      updatedConversation <- updateWith(event, lambdaService, dataService, cache, configuration)
      result <- updatedConversation.respond(event, lambdaService, dataService, cache, ws, configuration)
    } yield result
  }

  def toRaw: RawConversation = {
    RawConversation(id, trigger.id, triggerMessage, conversationType, context, maybeThreadId, userIdForContext, startedAt, state)
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

  val SECONDS_UNTIL_BACKGROUNDED = 60
}
