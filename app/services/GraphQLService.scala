package services

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import sangria.schema.Schema

import scala.concurrent.Future

trait GraphQLService {

  def schemaFor(groupVersion: BehaviorGroupVersion): Future[Schema[DefaultStorageItemService, Any]]

}
