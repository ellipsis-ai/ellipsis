package models.behaviors.config.awsconfig

import models.behaviors.behaviorversion.BehaviorVersion

import scala.concurrent.Future

trait AWSConfigService {

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[AWSConfig]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 maybeAccessKeyName: Option[String],
                 maybeSecretKeyName: Option[String],
                 maybeRegionName: Option[String]
               ): Future[AWSConfig]
}
