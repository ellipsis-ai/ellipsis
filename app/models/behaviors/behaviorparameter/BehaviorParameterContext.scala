package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import models.behaviors.SimpleTextResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, EventContext}
import models.behaviors.events.MessageActionConstants._
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class BehaviorParameterContext(
                                     event: Event,
                                     maybeConversation: Option[Conversation],
                                     behaviorVersion: BehaviorVersion,
                                     parameter: BehaviorParameter,
                                     services: DefaultServices
                                   ) {

  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  val eventContext: EventContext = event.eventContext

  def isFirstParamAction: DBIO[Boolean] = {
    services.dataService.behaviorParameters.isFirstForBehaviorVersionAction(parameter)
  }

  def unfilledParamCountAction(paramState: ParamCollectionState)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Int] = {
    maybeConversation.map { conversation =>
      paramState.allLeftToCollectAction(conversation).map(_.size)
    }.getOrElse(DBIO.successful(0))
  }

  def simpleTextResultFor(text: String): SimpleTextResult = {
    SimpleTextResult(
      event,
      maybeConversation,
      text,
      behaviorVersion.responseType
    )
  }

  def dataTypeChoiceCallbackId: String = dataTypeChoiceCallbackIdFor(event.eventContext.userIdForContext, maybeConversation.map(_.id))

  def yesNoCallbackId: String = yesNoCallbackIdFor(event.eventContext.userIdForContext, maybeConversation.map(_.id))

  def textInputCallbackId: String = textInputCallbackIdFor(event.eventContext.userIdForContext, maybeConversation.map(_.id))
}
