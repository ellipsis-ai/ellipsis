package json

import models.bots.config.RequiredOAuth2ApiConfig

case class RequiredOAuth2ApiConfigData(
                                        apiId: String,
                                        requiredScope: String,
                                        application: Option[OAuth2ApplicationData]
                                      )

object RequiredOAuth2ApiConfigData {
  def from(required: RequiredOAuth2ApiConfig): RequiredOAuth2ApiConfigData = {
    RequiredOAuth2ApiConfigData(
      required.api.id,
      required.requiredScope,
      required.maybeApplication.map(OAuth2ApplicationData.from)
    )
  }
}
