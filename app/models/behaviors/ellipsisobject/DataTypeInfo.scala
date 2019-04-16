package models.behaviors.ellipsisobject

import json.{BehaviorVersionData, InputData}

case class DataTypeInfo(
                         id: String,
                         name: Option[String],
                         inputs: Seq[InputInfo],
                         usesCode: Boolean,
                         defaultStorageFields: Seq[DefaultStorageFieldInfo]
                       )

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
