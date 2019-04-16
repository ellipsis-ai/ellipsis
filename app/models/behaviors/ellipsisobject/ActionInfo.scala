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
        InputInfo.allFrom(ea.inputIds, inputs)
      )
    }
  }
}
