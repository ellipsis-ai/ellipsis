package models.behaviors.behaviorparameter

import models.behaviors.SimpleTextResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.events.SlackMessageActionConstants._
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

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

  def unfilledParamCount(paramState: ParamCollectionState)(implicit ec: ExecutionContext): Future[Int] = {
    maybeConversation.map { conversation =>
      paramState.allLeftToCollect(conversation).map(_.size)
    }.getOrElse(Future.successful(0))
  }

  def simpleTextResultFor(text: String): SimpleTextResult = {
    SimpleTextResult(
      event,
      maybeConversation,
      text,
      behaviorVersion.forcePrivateResponse
    )
  }

  def inputChoiceCallbackId: String = inputChoiceCallbackIdFor(event.userIdForContext, maybeConversation.map(_.id))

}
