package models.behaviors.behaviorparameter

import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.cache.CacheApi
import services.{DefaultServices, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterContext(
                                     event: Event,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     services: DefaultServices
                                   ) {

  val behaviorVersion = parameter.behaviorVersion
  val dataService: DataService = services.dataService
  val cache: CacheApi = services.cache

  def isFirstParamAction: DBIO[Boolean] = {
    services.dataService.behaviorParameters.isFirstForBehaviorVersionAction(parameter)
  }

  def unfilledParamCount(paramState: ParamCollectionState): Future[Int] = {
    maybeConversation.map { conversation =>
      paramState.allLeftToCollect(conversation).map(_.size)
    }.getOrElse(Future.successful(0))
  }

}
