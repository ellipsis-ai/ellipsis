package services

import json.BehaviorGroupData
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.libs.json.JsValue
import sangria.schema.Schema

import scala.concurrent.Future

trait GraphQLService {

  def schemaFor(groupVersion: BehaviorGroupVersion): Future[Schema[DefaultStorageItemService, Any]]

  def previewSchemaFor(data: BehaviorGroupData): Future[Schema[DefaultStorageItemService, Any]]

  def runQuery(
                behaviorGroup: BehaviorGroup,
                query: String,
                maybeOperationName: Option[String],
                maybeVariables: Option[String]
              ): Future[JsValue]

}
