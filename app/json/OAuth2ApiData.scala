package json

import controllers.RemoteAssets.getUrl
import models.accounts.oauth2api.OAuth2Api

case class OAuth2ApiData(
                          apiId: String,
                          name: String,
                          requiresAuth: Boolean,
                          newApplicationUrl: Option[String],
                          scopeDocumentationUrl: Option[String],
                          imageUrl: Option[String]
                        )

object OAuth2ApiData {
  private def maybeImageUrlFor(apiName: String): Option[String] = {
    if (apiName.toLowerCase.contains("github")) {
      Some(getUrl("images/logos/GitHub-Mark-64px.png"))
    } else if (apiName.toLowerCase.contains("todoist")) {
      Some(getUrl("images/logos/todoist.png"))
    } else {
      None
    }
  }

  def from(api: OAuth2Api): OAuth2ApiData = {
    OAuth2ApiData(
      api.id,
      api.name,
      api.grantType.requiresAuth,
      api.maybeNewApplicationUrl,
      api.maybeScopeDocumentationUrl,
      this.maybeImageUrlFor(api.name)
    )
  }
}
