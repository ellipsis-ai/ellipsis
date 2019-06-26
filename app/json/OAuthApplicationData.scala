package json

import models.accounts.OAuthApplication

case class OAuthApplicationData(
                                  apiId: String,
                                  id: String,
                                  newApplicationUrl: Option[String],
                                  scope: Option[String],
                                  displayName: String
                                )

object OAuthApplicationData {
  def from(app: OAuthApplication): OAuthApplicationData = {
    OAuthApplicationData(app.api.id, app.id, app.maybeNewApplicationUrl, app.maybeScope, app.name)
  }
}
