package json

import models.behaviors.config.requiredawsconfig.RequiredAWSConfig

case class RequiredAWSConfigData(
                                  id: Option[String],
                                  config: Option[AWSConfigData]
                                ) {

  def copyForExport: RequiredAWSConfigData = {
    copy(id = None, config = None)
  }

}

object RequiredAWSConfigData {

  def from(config: RequiredAWSConfig): RequiredAWSConfigData = {
    RequiredAWSConfigData(Some(config.id), config.maybeConfig.map(AWSConfigData.from))
  }

}
