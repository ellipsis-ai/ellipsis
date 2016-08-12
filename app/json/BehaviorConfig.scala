package json

case class BehaviorConfig(
                           publishedId: Option[String],
                           aws: Option[AWSConfigData],
                           requiredOAuth2Applications: Option[Seq[OAuth2ApplicationData]]
                           ) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }
}
