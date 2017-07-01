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

  private def createItemForBehaviorAction(behavior: Behavior, data: JsValue): DBIO[DefaultStorageItem] = {
    val newID = IDs.next
    for {
      maybeCurrentVersion <- dataService.behaviors.maybeCurrentVersionForAction(behavior)
      maybeDataTypeConfig <- maybeCurrentVersion.map { version =>
        dataService.dataTypeConfigs.maybeForAction(version)
      }.getOrElse(DBIO.successful(None))
      fields <- maybeDataTypeConfig.map { config =>
        dataService.dataTypeFields.allForAction(config)
      }.getOrElse(DBIO.successful(Seq()))
      fieldsWithObjectType <- DBIO.successful(fields.filterNot(_.fieldType.isBuiltIn))
      nestedFieldItems <- DBIO.sequence(fieldsWithObjectType.flatMap { field =>
        (data \ field.name).toOption.map { fieldData =>
          createItemAction(field.fieldType.name, fieldData, behavior.group).map { maybeItem =>
            (field, maybeItem)
          }
        }
      })
      newData <- DBIO.successful(data match {
        case obj: JsObject => {
          val dataWithId: JsObject = obj + ("id", JsString(newID))
          nestedFieldItems.foldLeft(dataWithId)((acc, tuple) => {
            val (field, maybeItem) = tuple
            maybeItem.map { item =>
              acc + (field.name, JsString(item.id))
            }.getOrElse(acc)
          })
        }
        case _ => data
      })
      newInstance <- DBIO.successful(DefaultStorageItem(newID, behavior, newData))
      _ <- DBIO.successful(println(s"saving $newInstance"))
      _ <- (all += newInstance.toRaw)
    } yield newInstance
  }

  def createItemAction(typeName: String, data: JsValue, behaviorGroup: BehaviorGroup): DBIO[Option[DefaultStorageItem]] = {
    (for {
      maybeBehavior <- dataService.behaviors.findByNameAction(typeName, behaviorGroup)
      maybeItem <- maybeBehavior.map { behavior =>
        createItemForBehaviorAction(behavior, data).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeItem) transactionally
  }

  def createItem(typeName: String, data: JsValue, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]] = {
    dataService.run(createItemAction(typeName, data, behaviorGroup))
  }

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    Future.successful(null) // TODO: for realz
  }

}
