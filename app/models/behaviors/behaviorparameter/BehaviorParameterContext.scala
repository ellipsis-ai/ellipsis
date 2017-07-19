package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.Configuration
import services.{CacheService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterContext(
                                     event: Event,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cacheService: CacheService,
                                     dataService: DataService,
                                     configuration: Configuration,
                                     actorSystem: ActorSystem
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
