package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import models.behaviors.SimpleTextResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessageActionConstants._
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class BehaviorParameterContext(
                                     event: Event,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     services: DefaultServices
                                   ) {

  val behaviorVersion: BehaviorVersion = parameter.behaviorVersion
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

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

}
