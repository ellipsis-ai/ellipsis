package json.web.settings

import json._

case class IntegrationListConfig(
                                  containerId: String,
                                  csrfToken: Option[String],
                                  isAdmin: Boolean,
                                  teamId: String,
                                  oauthApis: Seq[OAuthApiData],
                                  oauthApplications: Seq[OAuthApplicationData],
                                  awsConfigs: Seq[AWSConfigData]
                          )
