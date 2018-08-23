package json

import models.accounts.oauth1application.OAuth1Application

case class OAuth1ApplicationData(
                                  apiId: String,
                                  id: String,
                                  displayName: String
                                )

object OAuth1ApplicationData {
  def from(app: OAuth1Application): OAuth1ApplicationData = {
    OAuth1ApplicationData(app.api.id, app.id, app.name)
  }
}
