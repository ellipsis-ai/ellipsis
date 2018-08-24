package models.behaviors.config.requiredoauth1apiconfig

import models.accounts.oauth1api.OAuth1Api
import models.accounts.oauth1application.OAuth1Application
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class RequiredOAuth1ApiConfig(
                                    id: String,
                                    exportId: String,
                                    groupVersion: BehaviorGroupVersion,
                                    api: OAuth1Api,
                                    maybeRecommendedScope: Option[String],
                                    nameInCode: String,
                                    maybeApplication: Option[OAuth1Application]
                                  ) {
  def isReady: Boolean = maybeApplication.isDefined

  def toRaw: RawRequiredOAuth1ApiConfig = {
    RawRequiredOAuth1ApiConfig(
      id,
      exportId,
      groupVersion.id,
      api.id,
      maybeRecommendedScope,
      nameInCode,
      maybeApplication.map(_.id)
    )
  }

}
