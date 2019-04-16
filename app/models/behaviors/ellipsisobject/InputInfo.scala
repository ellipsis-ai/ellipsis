package models.behaviors.ellipsisobject

import json.InputData
import models.behaviors.behaviorparameter.TextType

case class InputInfo(
                      name: String,
                      paramType: String,
                      question: String
                    )

object InputInfo {

  def allFrom(inputIds: Seq[String], inputs: Seq[InputData]): Seq[InputInfo] = {
    inputIds.flatMap(eaInputId => {
      val maybeData = inputs.find(_.inputId.contains(eaInputId))
      maybeData.map { input =>
        InputInfo(input.name, input.paramType.flatMap(_.id).getOrElse(TextType.id), input.question)
      }
    })
  }
}
