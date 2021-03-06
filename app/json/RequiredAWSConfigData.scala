package json

import models.behaviors.config.requiredawsconfig.RequiredAWSConfig

case class RequiredAWSConfigData(
                                  id: Option[String],
                                  exportId: Option[String],
                                  nameInCode: String,
                                  config: Option[AWSConfigData]
                                ) {

  def copyForExport: RequiredAWSConfigData = {
    copy(id = None, config = None)
  }

}

object RequiredAWSConfigData {

  def from(config: RequiredAWSConfig): RequiredAWSConfigData = {
    RequiredAWSConfigData(Some(config.id), Some(config.exportId), config.nameInCode, config.maybeConfig.map(AWSConfigData.from))
  }

}
