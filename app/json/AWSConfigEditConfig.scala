package json

case class AWSConfigEditConfig(
                                containerId: String,
                                csrfToken: Option[String],
                                isAdmin: Boolean,
                                teamId: String,
                                configId: String,
                                name: Option[String],
                                requiredNameInCode: Option[String],
                                accessKeyId: Option[String],
                                secretAccessKey: Option[String],
                                region: Option[String],
                                configSaved: Boolean,
                                documentationUrl: String,
                                behaviorGroupId: Option[String],
                                behaviorId: Option[String] = None
                              )
