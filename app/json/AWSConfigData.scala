package json

import models.behaviors.config.awsconfig.AWSConfig

case class AWSConfigData(
                          id: String,
                          displayName: String,
                          nameInCode: String,
                          accessKeyId: Option[String],
                          secretAccessKey: Option[String],
                          region: Option[String]
                          )

object AWSConfigData {

  def from(cfg: AWSConfig): AWSConfigData = {
    AWSConfigData(cfg.id, cfg.name, cfg.keyName, cfg.maybeAccessKey, cfg.maybeSecretKey, cfg.maybeRegion)
  }

}
