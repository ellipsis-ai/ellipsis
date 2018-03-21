package json

import models.behaviors.config.awsconfig.AWSConfig

case class AWSConfigData(
                          id: String,
                          displayName: String
                          )

object AWSConfigData {

  def from(cfg: AWSConfig): AWSConfigData = {
    AWSConfigData(cfg.id, cfg.name)
  }

}
