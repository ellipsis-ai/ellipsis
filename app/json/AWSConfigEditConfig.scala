package json

case class AWSConfigEditConfig(
                                containerId: String,
                                csrfToken: Option[String],
                                teamId: String,
                                configId: String,
                                name: Option[String],
                                accessKeyId: Option[String],
                                secretAccessKey: Option[String],
                                region: Option[String],
                                configSaved: Boolean,
                                documentationUrl: String,
                                behaviorId: Option[String] = None
                              )
