package models.behaviors.config.awsconfig

import models.environmentvariable.EnvironmentVariable

case class AWSConfig(
                      id: String,
                      behaviorVersionId: String,
                      maybeAccessKey: Option[EnvironmentVariable],
                      maybeSecretKey: Option[EnvironmentVariable],
                      maybeRegion: Option[EnvironmentVariable]
                    ) {
  def maybeAccessKeyName = maybeAccessKey.map(_.name)
  def maybeSecretKeyName = maybeSecretKey.map(_.name)
  def maybeRegionName = maybeRegion.map(_.name)

  def environmentVariableNames: Seq[String] = Seq(maybeAccessKeyName, maybeSecretKeyName, maybeRegionName).flatten
}
