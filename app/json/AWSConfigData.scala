package json

import models.behaviors.config.awsconfig.AWSConfig

case class AWSConfigData(
                          id: String,
                          accessKeyName: Option[String],
                          secretKeyName: Option[String],
                          regionName: Option[String]
                          ) {
  val knownEnvVarsUsed: Seq[String] = Seq(accessKeyName, secretKeyName, regionName).flatten
}

object AWSConfigData {

  def from(cfg: AWSConfig): AWSConfigData = {
    AWSConfigData(cfg.id, cfg.maybeAccessKey, cfg.maybeSecretKey, cfg.maybeRegion)
  }

}
