package json

case class AWSConfigEditConfig(
                                containerId: String,
                                csrfToken: Option[String],
                                teamId: String,
                                mainUrl: String,
                                configId: String,
                                name: Option[String] = None,
                                configSaved: Boolean = false,
                                behaviorId: Option[String] = None
                              )
