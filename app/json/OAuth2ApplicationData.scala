package json

import models.accounts.OAuth2Application

case class OAuth2ApplicationData(
                                  applicationId: String,
                                  displayName: String
                                  )

object OAuth2ApplicationData {

  def from(app: OAuth2Application): OAuth2ApplicationData = {
    OAuth2ApplicationData(app.id, app.name)
  }
}
