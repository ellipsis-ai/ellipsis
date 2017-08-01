package services

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.libs.json.JsValue
import sangria.schema.Schema

import scala.concurrent.Future

trait GraphQLService {

  def schemaFor(groupVersion: BehaviorGroupVersion, user: User): Future[Schema[DefaultStorageItemService, Any]]

  def previewSchemaFor(data: BehaviorGroupData): Future[Schema[DefaultStorageItemService, Any]]

  def runQuery(
                behaviorGroup: BehaviorGroup,
                user: User,
                query: String,
                maybeOperationName: Option[String],
                maybeVariables: Option[String]
              ): Future[JsValue]

}
