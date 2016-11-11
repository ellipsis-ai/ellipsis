package models.behaviors.config.requiredsimpletokenapi

import models.accounts.simpletokenapi.SimpleTokenApi
import models.behaviors.behaviorversion.BehaviorVersion

case class RequiredSimpleTokenApi(
                                    id: String,
                                    behaviorVersion: BehaviorVersion,
                                    api: SimpleTokenApi
                                  ) {
  def isReady: Boolean = true

  def toRaw: RawRequiredSimpleTokenApi = {
    RawRequiredSimpleTokenApi(
      id,
      behaviorVersion.id,
      api.id
    )
  }

}
