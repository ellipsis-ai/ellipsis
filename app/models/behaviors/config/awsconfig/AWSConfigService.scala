package models.behaviors.config.awsconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AWSConfigService {

  def maybeForAction(groupVersion: BehaviorGroupVersion): DBIO[Option[AWSConfig]]

  def maybeFor(groupVersion: BehaviorGroupVersion): Future[Option[AWSConfig]]

  def environmentVariablesUsedForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[String]] = {
    maybeForAction(groupVersion).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def createForAction(
                       groupVersion: BehaviorGroupVersion,
                       maybeAccessKeyName: Option[String],
                       maybeSecretKeyName: Option[String],
                       maybeRegionName: Option[String]
                     ): DBIO[AWSConfig]
}
