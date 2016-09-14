package models.bots.behaviorparameter

import models.bots.behaviorversion.BehaviorVersion

case class BehaviorParameter(
                              id: String,
                              name: String,
                              rank: Int,
                              behaviorVersion: BehaviorVersion,
                              maybeQuestion: Option[String],
                              paramType: BehaviorParameterType
                            ) {

  def question: String = maybeQuestion.getOrElse("")

  def isComplete: Boolean = {
    maybeQuestion.isDefined
  }

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, rank, behaviorVersion.id, maybeQuestion, paramType.name)
  }
}
