package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue

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

  def prompt(maybePreviousCollectedValue: Option[CollectedParameterValue]): Future[String] = {
    paramType.promptFor(this, maybePreviousCollectedValue)
  }

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, rank, behaviorVersion.id, maybeQuestion, paramType.name)
  }
}
