package json

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           aws: Option[AWSConfigData],
                           forcePrivateResponse: Option[Boolean],
                           isDataType: Boolean
                           ) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }

}
