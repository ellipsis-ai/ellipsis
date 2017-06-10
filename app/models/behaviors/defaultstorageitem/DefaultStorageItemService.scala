package models.behaviors.defaultstorageitem

import models.behaviors.behaviorgroup.BehaviorGroup

import scala.concurrent.Future

trait DefaultStorageItemService {

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]]

}
