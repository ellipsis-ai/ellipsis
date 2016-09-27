package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import play.api.cache.CacheApi
import services.DataService

import scala.concurrent.Future

case class BehaviorParameter(
                              id: String,
                              name: String,
                              rank: Int,
                              behaviorVersion: BehaviorVersion,
                              maybeQuestion: Option[String],
                              paramType: BehaviorParameterType
                            ) {

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def isComplete: Boolean = {
    maybeQuestion.isDefined
  }

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[CollectedParameterValue]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      s" (${paramType.invalidPromptModifier})"
    } else {
      ""
    }
  }

  def prompt(
              conversation: Conversation,
              maybePreviousCollectedValue: Option[CollectedParameterValue],
              event: MessageEvent,
              dataService: DataService,
              cache: CacheApi
            ): Future[String] = {
    paramType.promptFor(this, conversation, maybePreviousCollectedValue, event, dataService, cache)
  }

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, rank, behaviorVersion.id, maybeQuestion, paramType.name)
  }
}
