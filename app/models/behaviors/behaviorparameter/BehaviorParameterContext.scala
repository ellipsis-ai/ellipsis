package models.behaviors.behaviorparameter

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

  def paramCount: Future[Int] = dataService.behaviorParameters.allFor(behaviorVersion).map(_.size)

}
