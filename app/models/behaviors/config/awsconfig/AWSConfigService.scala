package models.behaviors.config.awsconfig

import models.behaviors.behaviorversion.BehaviorVersion
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait AWSConfigService {

  def maybeForAction(behaviorVersion: BehaviorVersion): DBIO[Option[AWSConfig]]

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[AWSConfig]]

  def environmentVariablesUsedForAction(behaviorVersion: BehaviorVersion)(implicit ec: ExecutionContext): DBIO[Seq[String]] = {
    maybeForAction(behaviorVersion).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def createForAction(
                 behaviorVersion: BehaviorVersion,
                 maybeAccessKeyName: Option[String],
                 maybeSecretKeyName: Option[String],
                 maybeRegionName: Option[String]
               ): DBIO[AWSConfig]
}
