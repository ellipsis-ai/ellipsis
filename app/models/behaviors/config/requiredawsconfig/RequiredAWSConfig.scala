package models.behaviors.config.requiredawsconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.config.awsconfig.AWSConfig

case class RequiredAWSConfig(
                            id: String,
                            requiredId: String,
                            nameInCode: String,
                            groupVersion: BehaviorGroupVersion,
                            maybeConfig: Option[AWSConfig]
                            ) {

  val isConfigured: Boolean = maybeConfig.isDefined

  def toRaw: RawRequiredAWSConfig = {
    RawRequiredAWSConfig(id, requiredId, nameInCode, groupVersion.id, maybeConfig.map(_.id))
  }
}
