package models.behaviors.defaultstorageitem

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.datatypefield.DataTypeField
import play.api.libs.json._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDefaultStorageItem(
                                  id: String,
                                  behaviorId: String,
                                  updatedAt: OffsetDateTime,
                                  updatedByUserId: String,
                                  data: JsValue
                                )

class DefaultStorageItemsTable(tag: Tag) extends Table[RawDefaultStorageItem](tag, "default_storage_items") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def updatedAt = column[OffsetDateTime]("updated_at")
  def updatedByUserId = column[String]("updated_by_user_id")
  def data = column[JsValue]("data")

  def * =
    (id, behaviorId, updatedAt, updatedByUserId, data) <> ((RawDefaultStorageItem.apply _).tupled, RawDefaultStorageItem.unapply _)
}

class CreationTypeNotFoundException extends Exception

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

  def searchByField(searchQuery: String, field: DataTypeField): Future[Seq[DefaultStorageItem]] = {
    for {
      maybeConfig <- dataService.dataTypeConfigs.find(field.configId)
      items <- maybeConfig.map { config =>
        val action = searchByFieldQuery(searchQuery, field.name, config.behaviorVersion.behavior.id).result.map { r =>
          r.map(tuple2Item)
        }
        dataService.run(action)
      }.getOrElse(Future.successful(Seq()))
    } yield items
  }

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]] = {
    val action = allForQuery(behavior.id).result.map { r =>
      r.map(tuple2Item)
    }
    dataService.run(action)
  }

  def countFor(behavior: Behavior): Future[Int] = {
    val action = countQuery(behavior.id).result
    dataService.run(action)
  }

  private def createItemForBehaviorAction(behavior: Behavior, user: User, data: JsValue): DBIO[DefaultStorageItem] = {
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
          createItemAction(field.fieldType.name, user, fieldData, behavior.group).map { maybeItem =>
            (field, maybeItem)
          }
        }
      })
      newData <- DBIO.successful(data match {
        case obj: JsObject => {
          val dataWithId: JsObject = obj + ("id", JsString(newID))
          nestedFieldItems.foldLeft(dataWithId)((acc, tuple) => {
            val (field, item) = tuple
            acc + (field.name, JsString(item.id))
          })
        }
        case _ => data
      })
      newInstance <- DBIO.successful(DefaultStorageItem(newID, behavior, OffsetDateTime.now, user.id, newData))
      _ <- DBIO.successful(println(s"saving $newInstance"))
      _ <- (all += newInstance.toRaw)
    } yield newInstance
  }

  def createItemForBehavior(behavior: Behavior, user: User, data: JsValue): Future[DefaultStorageItem] = {
    dataService.run(createItemForBehaviorAction(behavior, user, data))
  }

  def createItemAction(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): DBIO[DefaultStorageItem] = {
    ((for {
      maybeBehavior <- dataService.behaviors.findByNameAction(typeName, behaviorGroup)
      maybeItem <- maybeBehavior.map { behavior =>
        createItemForBehaviorAction(behavior, user, data).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeItem) transactionally).map { maybeNewItem =>
      maybeNewItem.getOrElse {
        throw new CreationTypeNotFoundException()
      }
    }
  }

  def createItem(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    dataService.run(createItemAction(typeName, user, data, behaviorGroup))
  }

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    Future.successful(null) // TODO: for realz
  }

}
