package models.behaviors.config.requiredoauth2apiconfig

import models.accounts.oauth2api.OAuth2Api
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredOAuth2ApiConfig(
                                    id: String,
                                    groupVersion: BehaviorGroupVersion,
                                    api: OAuth2Api,
                                    maybeRecommendedScope: Option[String],
                                    nameInCode: String,
                                    maybeApplication: Option[OAuth2Application]
                                  ) {
  // Could check scope too
  def isReady: Boolean = maybeApplication.isDefined

  def toRaw: RawRequiredOAuth2ApiConfig = {
    RawRequiredOAuth2ApiConfig(
      id,
      groupVersion.id,
      api.id,
      maybeRecommendedScope,
      nameInCode,
      maybeApplication.map(_.id)
    )
  }

}
