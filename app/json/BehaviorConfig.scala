package json

import export.BehaviorGroupExporter
import models.behaviors.behaviorversion.{Normal, Private}

case class LegacyBehaviorConfigJson(
                                     exportId: Option[String],
                                     name: Option[String],
                                     forcePrivateResponse: Option[Boolean], // Deprecated
                                     responseTypeId: Option[String],
                                     canBeMemoized: Option[Boolean],
                                     isDataType: Boolean,
                                     isTest: Option[Boolean],
                                     dataTypeConfig: Option[DataTypeConfigData]
                                   ) {
  def toBehaviorConfig: BehaviorConfig = {
    val definiteResponseTypeId = responseTypeId.getOrElse {
      if (forcePrivateResponse.contains(true)) {
        Private.id
      } else {
        Normal.id
      }
    }
    val withDefaultDataTypeConfig = if (isDataType && dataTypeConfig.isEmpty) {
      Some(DataTypeConfigData.withDefaultSettings)
    } else {
      dataTypeConfig
    }
    BehaviorConfig(
      exportId,
      name,
      definiteResponseTypeId,
      canBeMemoized,
      isDataType,
      isTest,
      withDefaultDataTypeConfig
    )
  }
}

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           responseTypeId: String,
                           canBeMemoized: Option[Boolean],
                           isDataType: Boolean,
                           isTest: Option[Boolean],
                           dataTypeConfig: Option[DataTypeConfigData]
                         ) {

  def copyForClone: BehaviorConfig = {
    copy(
      dataTypeConfig = dataTypeConfig.map(_.copyForClone)
    )
  }

  def copyForNewVersion: BehaviorConfig = {
    copy(
      dataTypeConfig = dataTypeConfig.map(_.copyForNewVersion)
    )
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorConfig = {
    copy(
      dataTypeConfig = dataTypeConfig.map(_.copyForExport(groupExporter))
    )
  }

}
