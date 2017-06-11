package models.behaviors.datatypeconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.defaultstorageitem.{DefaultStorageItem, DefaultStorageItemService}
import sangria.schema._
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataTypeConfigService {

  def graphQLTypeFor(
                      config: DataTypeConfig,
                      seen: scala.collection.mutable.Map[DataTypeConfig, ObjectType[DefaultStorageItemService, DefaultStorageItem]]
                    ): Future[ObjectType[DefaultStorageItemService, DefaultStorageItem]]

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[DataTypeConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]]

  def createForAction(behaviorVersion: BehaviorVersion): DBIO[DataTypeConfig]

}
