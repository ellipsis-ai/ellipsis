package models.behaviors.behaviorparameter

import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService
import services.slack.MessageEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterContext(
                                     event: MessageEvent,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cache: CacheApi,
                                     dataService: DataService,
                                     configuration: Configuration
                                   ) {

  val behaviorVersion = parameter.behaviorVersion

  def isFirstParam: Future[Boolean] = {
    dataService.behaviorParameters.isFirstForBehaviorVersion(parameter)
  }

  def unfilledParamCount(paramState: ParamCollectionState): Future[Int] = {
    maybeConversation.map { conversation =>
      paramState.allLeftToCollect(conversation).map(_.size)
    }.getOrElse(Future.successful(0))
  }

}
