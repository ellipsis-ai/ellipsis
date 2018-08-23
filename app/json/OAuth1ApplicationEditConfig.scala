package json

case class OAuth1ApplicationEditConfig(
                                        apis: Seq[OAuth1ApiData],
                                        callbackUrl: String,
                                        applicationClientId: Option[String] = None,
                                        applicationClientSecret: Option[String] = None
                                      )
