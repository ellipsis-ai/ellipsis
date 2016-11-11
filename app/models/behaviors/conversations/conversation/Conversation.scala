package models.behaviors.conversations.conversation

import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Conversation {
  val id: String
  val trigger: MessageTrigger
  val behaviorVersion: BehaviorVersion = trigger.behaviorVersion
  val conversationType: String
  val context: String
  val userIdForContext: String
  val startedAt: DateTime
  val state: String

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
    RawConversation(id, trigger.id, conversationType, context, userIdForContext, startedAt, state)
  }
}

object Conversation {
  val NEW_STATE = "new"
  val DONE_STATE = "done"

  val SLACK_CONTEXT = "slack"
  val API_CONTEXT = "api"

  val LEARN_BEHAVIOR = "learn_behavior"
  val INVOKE_BEHAVIOR = "invoke_behavior"
}
