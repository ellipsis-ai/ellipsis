package models.behaviors.defaultstorageitem

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDefaultStorageItem(id: String, behaviorGroupId: String, data: JsValue)

class DefaultStorageItemsTable(tag: Tag) extends Table[RawDefaultStorageItem](tag, "default_storage_items") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorGroupId = column[String]("behavior_group_id")
  def data = column[JsValue]("data")

  def * =
    (id, behaviorGroupId, data) <> ((RawDefaultStorageItem.apply _).tupled, RawDefaultStorageItem.unapply _)
}

class DefaultStorageItemServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends DefaultStorageItemService {

  def dataService = dataServiceProvider.get

  import DefaultStorageItemQueries._

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]] = {
    val action = findByIdQuery(id, behaviorGroup.id).result.map { r =>
      r.headOption.map(tuple2Item)
    }
    dataService.run(action)
  }

  def filter(filter: JsObject, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]] = {
    Future.successful(Seq()) // TODO: for realz
  }

}
