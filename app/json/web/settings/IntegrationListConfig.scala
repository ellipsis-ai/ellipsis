package json.web.settings

import json.{AWSConfigData, OAuth2ApiData, OAuth2ApplicationData}
import play.api.libs.json.Json

case class IntegrationListConfig(
                            containerId: String,
                            csrfToken: Option[String],
                            isAdmin: Boolean,
                            teamId: String,
                            apis: Seq[OAuth2ApiData],
                            applications: Seq[OAuth2ApplicationData],
                            awsConfigs: Seq[AWSConfigData]
                          )
