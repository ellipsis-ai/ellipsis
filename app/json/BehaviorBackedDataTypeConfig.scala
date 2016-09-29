package json

case class BehaviorBackedDataTypeConfig(
                                         name: String,
                                         publishedId: Option[String],
                                         aws: Option[AWSConfigData],
                                         requiredOAuth2ApiConfigs: Option[Seq[RequiredOAuth2ApiConfigData]]
                                       ) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }
}
object BehaviorBackedDataTypeConfig {

  def buildFrom(name: String, config: BehaviorConfig): BehaviorBackedDataTypeConfig = {
    BehaviorBackedDataTypeConfig(
      name,
      config.publishedId,
      config.aws,
      config.requiredOAuth2ApiConfigs
    )
  }

}
