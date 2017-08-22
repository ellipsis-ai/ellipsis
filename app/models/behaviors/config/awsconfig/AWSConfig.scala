package models.behaviors.config.awsconfig

import utils.NameFormatter

case class AWSConfig(
                      id: String,
                      name: String,
                      teamId: String,
                      maybeAccessKey: Option[String],
                      maybeSecretKey: Option[String],
                      maybeRegion: Option[String]
                    ) {

  def keyName: String = NameFormatter.formatConfigPropertyName(name)

  lazy val accessKey: String = maybeAccessKey.getOrElse("NOT_SET")
  lazy val secretKey: String = maybeSecretKey.getOrElse("NOT_SET")
  lazy val region: String = maybeRegion.getOrElse("NOT_SET")

  def environmentVariableNames: Seq[String] = Seq(maybeAccessKey, maybeSecretKey, maybeRegion).flatten
}
