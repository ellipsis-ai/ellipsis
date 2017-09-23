package json

import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig

case class RequiredOAuth2ApiConfigData(
                                        id: Option[String],
                                        apiId: String,
                                        recommendedScope: Option[String],
                                        nameInCode: String,
                                        config: Option[OAuth2ApplicationData]
                                      ) {

  def copyForExport: RequiredOAuth2ApiConfigData = {
    val maybeScope = recommendedScope.orElse(config.flatMap(_.scope))
    copy(id = None, config = None, recommendedScope = maybeScope)
  }

}

object RequiredOAuth2ApiConfigData {
  def from(required: RequiredOAuth2ApiConfig): RequiredOAuth2ApiConfigData = {
    RequiredOAuth2ApiConfigData(
      Some(required.id),
      required.api.id,
      required.maybeRecommendedScope,
      required.nameInCode,
      required.maybeApplication.map(OAuth2ApplicationData.from)
    )
  }
}
