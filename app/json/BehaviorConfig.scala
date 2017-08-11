package json

import export.BehaviorGroupExporter

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           aws: Option[AWSConfigData],
                           forcePrivateResponse: Option[Boolean],
                           isDataType: Boolean,
                           dataTypeConfig: Option[DataTypeConfigData]
                           ) {

  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }

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
