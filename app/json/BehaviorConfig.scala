package json

import export.BehaviorGroupExporter

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           responseType: BehaviorResponseTypeData,
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
