package json

import models.accounts.OAuthApplication

case class OAuthApplicationData(
                                  apiId: String,
                                  id: String,
                                  scope: Option[String],
                                  displayName: String
                                )

object OAuthApplicationData {
  def from(app: OAuthApplication): OAuthApplicationData = {
    OAuthApplicationData(app.api.id, app.id, app.maybeScope, app.name)
  }
}
