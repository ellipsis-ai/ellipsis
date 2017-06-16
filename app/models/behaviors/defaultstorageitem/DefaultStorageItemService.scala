package models.behaviors.defaultstorageitem

import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait DefaultStorageItemService {

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]]

  def filter(typeName: String, filter: JsValue, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]]

  def createItem(typeName: String, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem]

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem]

}
