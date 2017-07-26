package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.Configuration
import play.api.cache.CacheApi
import services.{DataService, SlackEventService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterContext(
                                     event: Event,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cache: CacheApi,
                                     dataService: DataService,
                                     slackEventService: SlackEventService,
                                     configuration: Configuration,
                                     actorSystem: ActorSystem
                                   ) {

  val behaviorVersion = parameter.behaviorVersion

  def isFirstParamAction: DBIO[Boolean] = {
    dataService.behaviorParameters.isFirstForBehaviorVersionAction(parameter)
  }

  def unfilledParamCount(paramState: ParamCollectionState): Future[Int] = {
    maybeConversation.map { conversation =>
      paramState.allLeftToCollect(conversation).map(_.size)
    }.getOrElse(Future.successful(0))
  }

}
