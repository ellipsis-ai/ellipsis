package json

import models.behaviors.config.RequiredOAuthApiConfig

case class RequiredOAuthApiConfigData(
                                        id: Option[String],
                                        exportId: Option[String],
                                        apiId: String,
                                        recommendedScope: Option[String],
                                        nameInCode: String,
                                        config: Option[OAuthApplicationData]
                                      ) {

  def copyForExport: RequiredOAuthApiConfigData = {
    copy(id = None, config = None)
  }

}

object RequiredOAuthApiConfigData {
  def from(required: RequiredOAuthApiConfig): RequiredOAuthApiConfigData = {
    RequiredOAuthApiConfigData(
      Some(required.id),
      Some(required.exportId),
      required.api.id,
      required.maybeRecommendedScope,
      required.nameInCode,
      required.maybeApplication.map(OAuthApplicationData.from)
    )
  }
}
