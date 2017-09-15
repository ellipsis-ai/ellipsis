package json

import models.accounts.oauth2application.OAuth2Application

case class OAuth2ApplicationData(
                                  apiId: String,
                                  id: String,
                                  scope: Option[String],
                                  displayName: String
                                  )

object OAuth2ApplicationData {
  def from(app: OAuth2Application): OAuth2ApplicationData = {
    OAuth2ApplicationData(app.api.id, app.id, app.maybeScope, app.name)
  }
}
