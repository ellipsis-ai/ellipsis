package json

case class AWSConfigListConfig(
                                containerId: String,
                                csrfToken: Option[String],
                                teamId: String,
                                configsData: Seq[AWSConfigData]
                              )
