package models.behaviors.config.requiredawsconfig

import json.RequiredAWSConfigData
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredAWSConfigService {

  def find(id: String): Future[Option[RequiredAWSConfig]]

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredAWSConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredAWSConfig]]

  def allFor(group: BehaviorGroup): Future[Seq[RequiredAWSConfig]]

  def save(requiredConfig: RequiredAWSConfig): Future[RequiredAWSConfig]

  def createForAction(data: RequiredAWSConfigData, groupVersion: BehaviorGroupVersion): DBIO[RequiredAWSConfig]

}
