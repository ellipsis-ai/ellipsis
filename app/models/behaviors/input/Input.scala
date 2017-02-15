package models.behaviors.input

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.BehaviorParameterType

case class Input(
                  id: String,
                  maybeExportId: Option[String],
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

  def toRaw: RawInput = {
    RawInput(id, maybeExportId, name, maybeQuestion, paramType.id, isSavedForTeam, isSavedForUser, maybeBehaviorGroup.map(_.id))
  }
}
