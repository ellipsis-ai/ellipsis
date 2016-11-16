package models.behaviors.input

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorParameterContext, BehaviorParameterType}

import scala.concurrent.Future

case class Input(
                  id: String,
                  name: String,
                  maybeQuestion: Option[String],
                  paramType: BehaviorParameterType,
                  isSavedForTeam: Boolean,
                  isSavedForUser: Boolean,
                  maybeBehaviorGroup: Option[BehaviorGroup]
                ) {

  val isSaved = isSavedForTeam || isSavedForUser

  def isShared = maybeBehaviorGroup.isDefined

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def prompt(maybeValue: Option[String], context: BehaviorParameterContext): Future[String] = {
    paramType.promptFor(maybeValue, context)
  }

  def toRaw: RawInput = {
    RawInput(id, name, maybeQuestion, paramType.name, isSavedForTeam, isSavedForUser, maybeBehaviorGroup.map(_.id))
  }
}
