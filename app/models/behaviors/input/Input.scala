package models.behaviors.input

import models.behaviors.behaviorparameter.{BehaviorParameterContext, BehaviorParameterType}
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue

import scala.concurrent.Future

case class Input(
                  id: String,
                  name: String,
                  maybeQuestion: Option[String],
                  paramType: BehaviorParameterType
                ) {

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def prompt(maybeCollected: Option[CollectedParameterValue], context: BehaviorParameterContext): Future[String] = {
    paramType.promptFor(maybeCollected, context)
  }

  def toRaw: RawInput = {
    RawInput(id, name, maybeQuestion, paramType.name)
  }
}
