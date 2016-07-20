package json

case class AWSConfigData(
                          accessKeyName: Option[String],
                          secretKeyName: Option[String],
                          regionName: Option[String]
                          ) {
  val knownEnvVarsUsed: Seq[String] = Seq(accessKeyName, secretKeyName, regionName).flatten
}
