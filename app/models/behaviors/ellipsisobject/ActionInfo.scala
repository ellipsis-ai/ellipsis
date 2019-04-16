package models.behaviors.ellipsisobject

import json.{BehaviorVersionData, InputData}

case class ActionInfo(
                       name: Option[String],
                       description: Option[String],
                       inputs: Seq[InputInfo]
                     )

object ActionInfo {

  def allFrom(versions: Seq[BehaviorVersionData], inputs: Seq[InputData]): Seq[ActionInfo] = {
    versions.map { ea =>
      ActionInfo(
        ea.name,
        ea.description,
        InputInfo.allFrom(ea.inputIds, inputs)
      )
    }
  }
}
