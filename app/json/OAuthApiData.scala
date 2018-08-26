package json

import controllers.RemoteAssets
import models.accounts.OAuthApi

case class OAuthApiData(
                          apiId: String,
                          name: String,
                          requiresAuth: Boolean,
                          newApplicationUrl: Option[String],
                          scopeDocumentationUrl: Option[String],
                          iconImageUrl: Option[String],
                          logoImageUrl: Option[String],
                          isOAuth1: Boolean
                        )

object OAuthApiData {
  private def maybeIconImageUrlFor(apiName: String, assets: RemoteAssets): Option[String] = {
    if (apiName.toLowerCase.contains("github")) {
      Some(assets.getUrl("images/logos/GitHub-Mark-64px.png"))
    } else if (apiName.toLowerCase.contains("todoist")) {
      Some(assets.getUrl("images/logos/todoist_icon.png"))
    } else {
      None
    }
  }

  private def maybeLogoImageUrlFor(apiName: String, assets: RemoteAssets): Option[String] = {
    if (apiName.toLowerCase.contains("todoist")) {
      Some(assets.getUrl("images/logos/todoist_logo.png"))
    } else if (apiName.toLowerCase.contains("yelp")) {
      Some(assets.getUrl("images/logos/yelp.png"))
    } else {
      None
    }
  }

  def from(api: OAuthApi, assets: RemoteAssets): OAuthApiData = {
    OAuthApiData(
      api.id,
      api.name,
      api.requiresAuth,
      api.maybeNewApplicationUrl,
      api.maybeScopeDocumentationUrl,
      this.maybeIconImageUrlFor(api.name, assets),
      this.maybeLogoImageUrlFor(api.name, assets),
      api.isOAuth1
    )
  }
}
