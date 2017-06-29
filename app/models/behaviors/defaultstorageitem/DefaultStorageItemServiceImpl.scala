package models.behaviors.defaultstorageitem

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDefaultStorageItem(id: String, behaviorId: String, data: JsValue)

class DefaultStorageItemsTable(tag: Tag) extends Table[RawDefaultStorageItem](tag, "default_storage_items") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def data = column[JsValue]("data")

  def * =
    (id, behaviorId, data) <> ((RawDefaultStorageItem.apply _).tupled, RawDefaultStorageItem.unapply _)
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

  private def filterForBehavior(behavior: Behavior, filter: JsValue): Future[Seq[DefaultStorageItem]] = {
    val action = filterQuery(behavior.id, filter).result.map { r =>
      r.map(tuple2Item)
    }
    dataService.run(action)
  }

  def filter(typeName: String, filter: JsValue, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]] = {
    dataService.behaviors.findByIdOrName(typeName, behaviorGroup).flatMap { maybeBehavior =>
      maybeBehavior.map { behavior =>
        filterForBehavior(behavior, filter)
      }.getOrElse(Future.successful(Seq()))
    }
  }

  private def createItemForBehavior(behavior: Behavior, data: JsValue): Future[DefaultStorageItem] = {
    val newID = IDs.next
    val newData = data match {
      case obj: JsObject => obj + ("id", JsString(newID))
      case _ => data
    }
    val newInstance = DefaultStorageItem(newID, behavior, newData)
    val action = (all += newInstance.toRaw).map(_ => newInstance)
    dataService.run(action)
  }

  def createItem(typeName: String, data: JsValue, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]] = {
    dataService.behaviors.findByIdOrName(typeName, behaviorGroup).flatMap { maybeBehavior =>
      maybeBehavior.map { behavior =>
        createItemForBehavior(behavior, data).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    Future.successful(null) // TODO: for realz
  }

}
