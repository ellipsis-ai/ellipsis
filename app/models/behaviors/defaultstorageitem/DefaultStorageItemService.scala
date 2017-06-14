package models.behaviors.defaultstorageitem

import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait DefaultStorageItemService {

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]]

  def filter(filter: JsObject, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]]

}
