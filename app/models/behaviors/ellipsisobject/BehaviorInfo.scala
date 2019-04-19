package models.behaviors.ellipsisobject

import json.{BehaviorVersionData, InputData}

sealed trait BehaviorInfo {
  val id: String
  val name: Option[String]
  val inputs: Seq[InputInfo]
}

case class ActionInfo(
                       id: String,
                       name: Option[String],
                       description: Option[String],
                       inputs: Seq[InputInfo]
                     ) extends BehaviorInfo

object ActionInfo {

  def allFrom(versions: Seq[BehaviorVersionData], inputs: Seq[InputData]): Seq[ActionInfo] = {
    versions.flatMap { ea =>
      ea.behaviorId.map { behaviorId =>
        ActionInfo(
          behaviorId,
          ea.name,
          ea.description,
          InputInfo.allFrom(ea.inputIds, inputs)
        )
      }
    }
  }
}

case class DataTypeInfo(
                         id: String,
                         name: Option[String],
                         inputs: Seq[InputInfo],
                         usesCode: Boolean,
                         defaultStorageFields: Seq[DefaultStorageFieldInfo]
                       ) extends BehaviorInfo

object DataTypeInfo {

  def allFrom(versions: Seq[BehaviorVersionData], inputs: Seq[InputData]): Seq[DataTypeInfo] = {
    versions.flatMap { ea =>
      for {
        behaviorId <- ea.behaviorId
        config <- ea.config.dataTypeConfig
      } yield {
        DataTypeInfo(
          behaviorId,
          ea.name,
          InputInfo.allFrom(ea.inputIds, inputs),
          config.usesCode.exists(identity),
          config.fields.map(DefaultStorageFieldInfo.fromFieldData)
        )
      }
    }
  }

}
