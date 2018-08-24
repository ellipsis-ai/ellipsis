package json

case class OAuth1ApplicationEditConfig(
                                        apis: Seq[OAuth1ApiData],
                                        callbackUrl: String,
                                        applicationConsumerKey: Option[String] = None,
                                        applicationConsumerSecret: Option[String] = None
                                      )
