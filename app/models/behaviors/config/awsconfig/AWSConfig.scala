package models.behaviors.config.awsconfig

case class AWSConfig(
                      id: String,
                      groupVersionId: String,
                      maybeAccessKeyName: Option[String],
                      maybeSecretKeyName: Option[String],
                      maybeRegionName: Option[String]
                    ) {

  def environmentVariableNames: Seq[String] = Seq(maybeAccessKeyName, maybeSecretKeyName, maybeRegionName).flatten
}
