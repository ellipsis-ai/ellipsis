package json

import models.accounts.OAuth2Api

case class OAuth2ApiData(
                          apiId: String,
                          name: String
                        )

object OAuth2ApiData {
  def from(api: OAuth2Api): OAuth2ApiData = {
    OAuth2ApiData(api.id, api.name)
  }
}
