package models.behaviors.defaultstorageitem

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.datatypefield.DataTypeField
import play.api.libs.json.JsValue
import slick.dbio.DBIO

import scala.concurrent.Future

trait DefaultStorageItemService {

  def findById(id: String, groupVersion: BehaviorGroupVersion): Future[Option[DefaultStorageItem]]

  def filter(typeName: String, filter: JsValue, groupVersion: BehaviorGroupVersion): Future[Seq[DefaultStorageItem]]

  def countForAction(behavior: Behavior): DBIO[Int]

  def countFor(behavior: Behavior): Future[Int]

  def searchByFieldAction(searchQuery: String, field: DataTypeField): DBIO[Seq[DefaultStorageItem]]

  def searchByField(searchQuery: String, field: DataTypeField): Future[Seq[DefaultStorageItem]]

  def allForAction(behavior: Behavior): DBIO[Seq[DefaultStorageItem]]

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]]

  def createItemForBehaviorVersion(behaviorVersion: BehaviorVersion, user: User, data: JsValue): Future[DefaultStorageItem]

  def createItem(typeName: String, user: User, data: JsValue, groupVersion: BehaviorGroupVersion): Future[DefaultStorageItem]

  def updateItem(typeName: String, user: User, data: JsValue, groupVersion: BehaviorGroupVersion): Future[DefaultStorageItem]

  def deleteItem(id: String, groupVersion: BehaviorGroupVersion): Future[Option[DefaultStorageItem]]

  def deleteItems(ids: Seq[String], groupVersion: BehaviorGroupVersion): Future[Int]

  def deleteFilteredItemsFor(typeName: String, filter: JsValue, groupVersion: BehaviorGroupVersion): Future[Seq[DefaultStorageItem]]

}
