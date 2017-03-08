package models.behaviors.input

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.BehaviorParameterType

case class Input(
                  id: String,
                  maybeInputId: Option[String],
                  maybeExportId: Option[String],
                  name: String,
                  maybeQuestion: Option[String],
                  paramType: BehaviorParameterType,
                  isSavedForTeam: Boolean,
                  isSavedForUser: Boolean,
                  behaviorGroupVersion: BehaviorGroupVersion
                ) {

  val inputId = maybeInputId.get

  val isSaved = isSavedForTeam || isSavedForUser

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def toRaw: RawInput = {
    RawInput(id, maybeInputId, maybeExportId, name, maybeQuestion, paramType.id, isSavedForTeam, isSavedForUser, behaviorGroupVersion.id)
  }
}
