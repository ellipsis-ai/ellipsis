package json.web.settings

import json._

case class IntegrationListConfig(
                                  containerId: String,
                                  csrfToken: Option[String],
                                  isAdmin: Boolean,
                                  teamId: String,
                                  oauth1Apis: Seq[OAuth1ApiData],
                                  oauth1Applications: Seq[OAuth1ApplicationData],
                                  oauth2Apis: Seq[OAuth2ApiData],
                                  oauth2Applications: Seq[OAuth2ApplicationData],
                                  awsConfigs: Seq[AWSConfigData]
                          )
