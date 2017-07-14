package models.behaviors.defaultstorageitem

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.datatypefield.DataTypeField
import play.api.libs.json.JsValue
import slick.dbio.DBIO

import scala.concurrent.Future

trait DefaultStorageItemService {

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]]

  def filter(typeName: String, filter: JsValue, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]]

  def countFor(behavior: Behavior): Future[Int]

  def searchByField(searchQuery: String, field: DataTypeField): Future[Seq[DefaultStorageItem]]

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]]

  def createItem(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem]

  def createItemAction(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): DBIO[DefaultStorageItem]

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem]

}
