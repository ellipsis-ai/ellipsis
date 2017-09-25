package json

import models.behaviors.config.awsconfig.AWSConfig

case class AWSConfigData(
                          id: String,
                          displayName: String,
                          accessKeyId: Option[String],
                          secretAccessKey: Option[String],
                          region: Option[String]
                          )

object AWSConfigData {

  def from(cfg: AWSConfig): AWSConfigData = {
    AWSConfigData(cfg.id, cfg.name, cfg.maybeAccessKey, cfg.maybeSecretKey, cfg.maybeRegion)
  }

}
