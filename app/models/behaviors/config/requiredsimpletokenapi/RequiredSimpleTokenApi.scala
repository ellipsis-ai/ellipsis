package models.behaviors.config.requiredsimpletokenapi

import models.accounts.simpletokenapi.SimpleTokenApi
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredSimpleTokenApi(
                                   id: String,
                                   requiredId: String,
                                   groupVersion: BehaviorGroupVersion,
                                   nameInCode: String,
                                   api: SimpleTokenApi
                                  ) {
  def isReady: Boolean = true

  def toRaw: RawRequiredSimpleTokenApi = {
    RawRequiredSimpleTokenApi(
      id,
      requiredId,
      groupVersion.id,
      nameInCode,
      api.id
    )
  }

}
