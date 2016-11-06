package models.behaviors.config.awsconfig

import models.behaviors.behaviorversion.BehaviorVersion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AWSConfigService {

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[AWSConfig]]

  def environmentVariablesUsedFor(behaviorVersion: BehaviorVersion): Future[Seq[String]] = {
    maybeFor(behaviorVersion).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 maybeAccessKeyName: Option[String],
                 maybeSecretKeyName: Option[String],
                 maybeRegionName: Option[String]
               ): Future[AWSConfig]
}
