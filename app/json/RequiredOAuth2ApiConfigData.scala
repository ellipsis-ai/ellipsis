package json

import models.bots.config.RequiredOAuth2ApiConfig

case class RequiredOAuth2ApiConfigData(
                                        id: String,
                                        apiId: String,
                                        recommendedScope: Option[String],
                                        application: Option[OAuth2ApplicationData]
                                      )

object RequiredOAuth2ApiConfigData {
  def from(required: RequiredOAuth2ApiConfig): RequiredOAuth2ApiConfigData = {
    RequiredOAuth2ApiConfigData(
      required.id,
      required.api.id,
      required.maybeRecommendedScope,
      required.maybeApplication.map(OAuth2ApplicationData.from)
    )
  }
}
