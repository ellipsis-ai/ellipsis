package models.behaviors.datatypefield

import models.behaviors.datatypeconfig.DataTypeConfig
import models.behaviors.defaultstorageitem.{DefaultStorageItem, DefaultStorageItemService}
import sangria.schema.{Field, ObjectType}

import scala.concurrent.Future

trait DataTypeFieldService {

  def graphQLFor(
                  field: DataTypeField,
                  seen: scala.collection.mutable.Map[DataTypeConfig, ObjectType[DefaultStorageItemService, DefaultStorageItem]]
                ): Future[Field[DefaultStorageItemService, DefaultStorageItem]]

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]]

}
