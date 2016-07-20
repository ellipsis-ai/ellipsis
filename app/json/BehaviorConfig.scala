package json

case class BehaviorConfig(publishedId: Option[String], aws: Option[AWSConfigData]) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }
}
