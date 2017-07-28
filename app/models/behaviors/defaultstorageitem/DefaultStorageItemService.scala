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

  def countForAction(behavior: Behavior): DBIO[Int]

  def countFor(behavior: Behavior): Future[Int]

  def searchByFieldAction(searchQuery: String, field: DataTypeField): DBIO[Seq[DefaultStorageItem]]

  def searchByField(searchQuery: String, field: DataTypeField): Future[Seq[DefaultStorageItem]]

  def allForAction(behavior: Behavior): DBIO[Seq[DefaultStorageItem]]

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]]

  def createItemForBehavior(behavior: Behavior, user: User, data: JsValue): Future[DefaultStorageItem]

  def createItem(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem]

  def createItemAction(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): DBIO[DefaultStorageItem]

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]]

}
