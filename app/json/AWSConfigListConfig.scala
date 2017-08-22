package json

case class AWSConfigListConfig(
                                containerId: String,
                                csrfToken: Option[String],
                                teamId: String,
                                configs: Seq[AWSConfigData]
                              )
