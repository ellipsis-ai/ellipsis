package models.behaviors.config.requiredsimpletokenapi

import models.accounts.simpletokenapi.SimpleTokenApi
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredSimpleTokenApi(
                                   id: String,
                                   groupVersion: BehaviorGroupVersion,
                                   api: SimpleTokenApi
                                  ) {
  def isReady: Boolean = true

  def toRaw: RawRequiredSimpleTokenApi = {
    RawRequiredSimpleTokenApi(
      id,
      groupVersion.id,
      api.id
    )
  }

}
