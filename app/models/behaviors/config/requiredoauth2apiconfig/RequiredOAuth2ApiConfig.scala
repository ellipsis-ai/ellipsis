package models.behaviors.config.requiredoauth2apiconfig

import models.accounts.oauth2api.OAuth2Api
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.behaviorversion.BehaviorVersion

case class RequiredOAuth2ApiConfig(
                                    id: String,
                                    behaviorVersion: BehaviorVersion,
                                    api: OAuth2Api,
                                    maybeRecommendedScope: Option[String],
                                    maybeApplication: Option[OAuth2Application]
                                  ) {
  // Could check scope too
  def isReady: Boolean = maybeApplication.isDefined

  def toRaw: RawRequiredOAuth2ApiConfig = {
    RawRequiredOAuth2ApiConfig(
      id,
      behaviorVersion.id,
      api.id,
      maybeRecommendedScope,
      maybeApplication.map(_.id)
    )
  }

}
