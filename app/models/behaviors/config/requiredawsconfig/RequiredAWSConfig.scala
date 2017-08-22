package models.behaviors.config.requiredawsconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.config.awsconfig.AWSConfig

case class RequiredAWSConfig(
                            id: String,
                            groupVersion: BehaviorGroupVersion,
                            maybeConfig: Option[AWSConfig]
                            ) {

  def toRaw: RawRequiredAWSConfig = {
    RawRequiredAWSConfig(id, groupVersion.id, maybeConfig.map(_.id))
  }
}
