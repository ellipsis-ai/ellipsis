package json

import models.behaviors.config.requiredawsconfig.RequiredAWSConfig

case class RequiredAWSConfigData(
                                  id: Option[String],
                                  nameInCode: String
                                ) {

  def copyForExport: RequiredAWSConfigData = {
    copy(id = None)
  }

}

object RequiredAWSConfigData {

  def from(config: RequiredAWSConfig): RequiredAWSConfigData = {
    RequiredAWSConfigData(Some(config.id), config.nameInCode)
  }

}
