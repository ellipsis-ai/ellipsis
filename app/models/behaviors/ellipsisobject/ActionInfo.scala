package models.behaviors.ellipsisobject

import json.{BehaviorVersionData, InputData}

case class ActionInfo(
                       name: Option[String],
                       description: Option[String],
                       functionBody: String,
                       responseTemplate: String,
                       inputs: Seq[InputInfo]
                     )

object ActionInfo {

  def allFrom(versions: Seq[BehaviorVersionData], inputs: Seq[InputData]): Seq[ActionInfo] = {
    versions.map { ea =>
      ActionInfo(
        ea.name,
        ea.description,
        ea.functionBody,
        ea.responseTemplate,
        ea.inputIds.flatMap(eaInputId => {
          val maybeData = inputs.find(_.inputId.contains(eaInputId))
          maybeData.map { input =>
            InputInfo(input.name, input.paramType.map(_.name), input.question)
          }
        })
      )
    }
  }
}
