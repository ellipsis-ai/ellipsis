package json

import models.accounts.OAuth2Application

case class OAuth2ApplicationData(
                                  apiId: String,
                                  applicationId: String,
                                  displayName: String,
                                  keyName: String
                                  )

object OAuth2ApplicationData {
  def from(app: OAuth2Application): OAuth2ApplicationData = {
    OAuth2ApplicationData(app.api.id, app.id, app.name, app.keyName)
  }
}
