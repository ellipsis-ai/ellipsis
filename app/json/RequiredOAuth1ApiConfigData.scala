package json

import models.behaviors.config.requiredoauth1apiconfig.RequiredOAuth1ApiConfig

case class RequiredOAuth1ApiConfigData(
                                        id: Option[String],
                                        exportId: Option[String],
                                        apiId: String,
                                        nameInCode: String,
                                        config: Option[OAuth1ApplicationData]
                                      ) {

  def copyForExport: RequiredOAuth1ApiConfigData = {
    copy(id = None, config = None)
  }

}

object RequiredOAuth1ApiConfigData {
  def from(required: RequiredOAuth1ApiConfig): RequiredOAuth1ApiConfigData = {
    RequiredOAuth1ApiConfigData(
      Some(required.id),
      Some(required.exportId),
      required.api.id,
      required.nameInCode,
      required.maybeApplication.map(OAuth1ApplicationData.from)
    )
  }
}
