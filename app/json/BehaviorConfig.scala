package json

case class BehaviorConfig(
                           publishedId: Option[String],
                           aws: Option[AWSConfigData],
                           requiredOAuth2ApiConfigs: Option[Seq[RequiredOAuth2ApiConfigData]]
                           ) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }
}
