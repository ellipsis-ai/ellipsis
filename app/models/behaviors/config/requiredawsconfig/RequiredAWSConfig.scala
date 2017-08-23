package models.behaviors.config.requiredawsconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredAWSConfig(
                            id: String,
                            nameInCode: String,
                            groupVersion: BehaviorGroupVersion
                            ) {

  def toRaw: RawRequiredAWSConfig = {
    RawRequiredAWSConfig(id, nameInCode, groupVersion.id)
  }
}
