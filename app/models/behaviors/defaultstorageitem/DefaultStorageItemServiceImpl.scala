package models.behaviors.defaultstorageitem

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BuiltInType}
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

  def tuple2Item(tuple: TupleType): DBIO[DefaultStorageItem] = {
    val raw = tuple._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._2)
    dataWithFieldNamesFor(raw.id, raw.data, raw.behaviorId).map { dataWithNames =>
      DefaultStorageItem(raw.id, behavior, raw.updatedAt, raw.updatedByUserId, dataWithNames)
    }
  }

  private def fieldsForBehaviorIdAction(behaviorId: String): DBIO[Seq[DataTypeField]] = {
    for {
      maybeBehavior <- dataService.behaviors.findWithoutAccessCheckAction(behaviorId)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        dataService.behaviors.maybeCurrentVersionForAction(behavior)
      }.getOrElse(DBIO.successful(None))
      maybeDataTypeConfig <- maybeBehaviorVersion.map { version =>
        dataService.dataTypeConfigs.maybeForAction(version)
      }.getOrElse(DBIO.successful(None))
      fields <- maybeDataTypeConfig.map { config =>
        dataService.dataTypeFields.allForAction(config)
      }.getOrElse(DBIO.successful(Seq()))
    } yield fields
  }

  def findByIdAction(id: String, behaviorGroup: BehaviorGroup): DBIO[Option[DefaultStorageItem]] = {
    for {
      maybeTuple <- findByIdQuery(id, behaviorGroup.id).result.map(_.headOption)
      maybeItem <- maybeTuple.map(t => tuple2Item(t).map(Some(_))).getOrElse(DBIO.successful(None))
    } yield maybeItem
  }

  def findById(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]] = {
    dataService.run(findByIdAction(id, behaviorGroup))
  }

  private def filterForBehavior(behavior: Behavior, filter: JsValue): Future[Seq[DefaultStorageItem]] = {
    val action = for {
      filterWithIds <- dataWithFieldIdsFor(filter, behavior.id)
      result <- filterQuery(behavior.id, filterWithIds).result
      items <- DBIO.sequence(result.map(tuple2Item))
    } yield items
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
        val behaviorId = config.behaviorVersion.behavior.id
        val action = for {
          result <- searchByFieldQuery(searchQuery, field.name, behaviorId).result
          items <- DBIO.sequence(result.map(tuple2Item))
        } yield items
        dataService.run(action)
      }.getOrElse(Future.successful(Seq()))
    } yield items
  }

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]] = {
    val action = for {
      result <- allForQuery(behavior.id).result
      items <- DBIO.sequence(result.map(tuple2Item))
    } yield items
    dataService.run(action)
  }

  def countFor(behavior: Behavior): Future[Int] = {
    val action = countQuery(behavior.id).result
    dataService.run(action)
  }

  private def fieldValueWithIdsFor(field: DataTypeField, fieldValue: JsValue): DBIO[JsValue] = {
    field.fieldType match {
      case t: BuiltInType => DBIO.successful(t.prepareJsValue(fieldValue))
      case t: BehaviorBackedDataType => dataWithFieldIdsFor(fieldValue, t.behaviorVersion.behavior.id)
    }
  }

  private def dataWithFieldIdsFor(data: JsValue, behaviorId: String): DBIO[JsValue] = {
    data match {
      case obj: JsObject => {
        fieldsForBehaviorIdAction(behaviorId).flatMap { fields =>
          val initial: DBIO[JsObject] = DBIO.successful(Json.obj())
          obj.value.foldLeft(initial) {
            case (eventualAcc, (fieldName, fieldValue)) => {
              eventualAcc.flatMap { acc =>
                fields.find(_.name == fieldName).map { field =>
                  fieldValueWithIdsFor(field, fieldValue).map { resolvedValue =>
                    acc + (field.fieldId, resolvedValue)
                  }
                }.getOrElse(DBIO.successful(acc))
              }
            }
          }
        }.map(v => v - "id")
      }
      case _ => DBIO.successful(data)
    }
  }


  private def fieldValueWithNamesFor(field: DataTypeField, fieldValue: JsValue): DBIO[JsValue] = {
    field.fieldType match {
      case t: BuiltInType => DBIO.successful(fieldValue)
      case t: BehaviorBackedDataType => {
        fieldValue match {
          case id: JsString => findByIdAction(id.value, t.behaviorVersion.behavior.group).map { maybeItem =>
            maybeItem.map(_.data).getOrElse(id)
          }
          case _ => DBIO.successful(fieldValue)
        }
      }
    }
  }

  private def dataWithFieldNamesFor(id: String, data: JsValue, behaviorId: String): DBIO[JsValue] = {
    data match {
      case obj: JsObject => {
        fieldsForBehaviorIdAction(behaviorId).flatMap { fields =>
          val initial: DBIO[JsObject] = DBIO.successful(Json.obj())
          obj.value.foldLeft(initial) {
            case (eventualAcc, (fieldId, fieldValue)) => {
              eventualAcc.flatMap { acc =>
                fields.find(_.fieldId == fieldId).map { field =>
                  fieldValueWithNamesFor(field, fieldValue).map { resolvedValue =>
                    acc + (field.name, resolvedValue)
                  }
                }.getOrElse(DBIO.successful(acc))
              }
            }
          }
        }.map(v => v + ("id", JsString(id)))
      }
      case _ => DBIO.successful(data)
    }
  }

  private def createItemForBehaviorAction(behavior: Behavior, user: User, data: JsValue): DBIO[DefaultStorageItem] = {
    val newID = IDs.next
    val team = behavior.team
    val group = behavior.group
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
          nestedFieldItems.foldLeft(obj)((acc, tuple) => {
            val (field, item) = tuple
            acc + (field.name, JsString(item.id))
          })
        }
        case _ => data
      })
      newDataWithFieldIds <- dataWithFieldIdsFor(newData, behavior.id)
      newInstanceToSave <- DBIO.successful(DefaultStorageItem(newID, behavior, OffsetDateTime.now, user.id, newDataWithFieldIds))
      _ <- DBIO.successful(println(s"saving $newInstanceToSave"))
      _ <- (all += newInstanceToSave.toRaw)
      newInstanceToReturn <- tuple2Item((newInstanceToSave.toRaw, ((behavior.toRaw, team), Some((group.toRaw, team)))))
    } yield newInstanceToReturn
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
