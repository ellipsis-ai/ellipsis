package json

import controllers.RemoteAssets.getUrl
import models.accounts.simpletokenapi.SimpleTokenApi

case class SimpleTokenApiData(
                               apiId: String,
                               name: String,
                               tokenUrl: Option[String],
                               iconImageUrl: Option[String],
                               logoImageUrl: Option[String]
                        )

object SimpleTokenApiData {

  private def maybeLogoImageUrlFor(apiName: String): Option[String] = {
    if (apiName.toLowerCase.contains("pivotal tracker")) {
      Some(getUrl("images/logos/pivotal_tracker.png"))
    } else {
      None
    }
  }

  def from(api: SimpleTokenApi): SimpleTokenApiData = {
    SimpleTokenApiData(
      api.id,
      api.name,
      api.maybeTokenUrl,
      None,
      this.maybeLogoImageUrlFor(api.name)
    )
  }

}
