package models.behaviors.config.requiredsimpletokenapi

import models.accounts.simpletokenapi.SimpleTokenApi
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredSimpleTokenApi(
                                   id: String,
                                   groupVersion: BehaviorGroupVersion,
                                   nameInCode: String,
                                   api: SimpleTokenApi
                                  ) {
  def isReady: Boolean = true

  def toRaw: RawRequiredSimpleTokenApi = {
    RawRequiredSimpleTokenApi(
      id,
      groupVersion.id,
      nameInCode,
      api.id
    )
  }

}
