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
import sangria.execution.UserFacingError
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

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

case class CreationTypeNotFoundException(typeName: String) extends Exception with UserFacingError {
  override def getMessage(): String = s"Can't create item: type with name `$typeName` not found"
}
case class IdPassedForCreationException(id: String) extends Exception with UserFacingError {
  override def getMessage(): String = s"You can't pass in an ID when creating an item. An ID will be created for you."
}
class NoIdPassedForUpdateException extends Exception with UserFacingError {
  override def getMessage(): String = "You must include an ID when attempting to update an item"
}

class DefaultStorageItemServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             implicit val ec: ExecutionContext
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

  private def filterForBehaviorAction(behavior: Behavior, filter: JsValue): DBIO[Seq[DefaultStorageItem]] = {
    for {
      fields <- fieldsForBehaviorIdAction(behavior.id)
      filterWithIds <- dataWithFieldIdsFor(filter, fields)
      result <- filterQuery(behavior.id, filterWithIds).result
      items <- DBIO.sequence(result.map(tuple2Item))
    } yield items
  }

  private def filterForBehavior(behavior: Behavior, filter: JsValue): Future[Seq[DefaultStorageItem]] = {
    dataService.run(filterForBehaviorAction(behavior, filter))
  }

  def filter(typeName: String, filter: JsValue, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]] = {
    dataService.behaviors.findByIdOrName(typeName, behaviorGroup).flatMap { maybeBehavior =>
      maybeBehavior.map { behavior =>
        filterForBehavior(behavior, filter)
      }.getOrElse(Future.successful(Seq()))
    }
  }

  def searchByFieldAction(searchQuery: String, field: DataTypeField): DBIO[Seq[DefaultStorageItem]] = {
    for {
      maybeConfig <- dataService.dataTypeConfigs.findAction(field.configId)
      items <- maybeConfig.map { config =>
        val behaviorId = config.behaviorVersion.behavior.id
        for {
          result <- searchByFieldQuery(searchQuery, field.name, behaviorId).result
          items <- DBIO.sequence(result.map(tuple2Item))
        } yield items
      }.getOrElse(DBIO.successful(Seq()))
    } yield items
  }

  def searchByField(searchQuery: String, field: DataTypeField): Future[Seq[DefaultStorageItem]] = {
    dataService.run(searchByFieldAction(searchQuery, field))
  }

  def allForAction(behavior: Behavior): DBIO[Seq[DefaultStorageItem]] = {
    for {
      result <- allForQuery(behavior.id).result
      items <- DBIO.sequence(result.map(tuple2Item))
    } yield items
  }

  def allFor(behavior: Behavior): Future[Seq[DefaultStorageItem]] = {
    dataService.run(allForAction(behavior))
  }

  def countForAction(behavior: Behavior): DBIO[Int] = {
    countQuery(behavior.id).result
  }

  def countFor(behavior: Behavior): Future[Int] = {
    dataService.run(countForAction(behavior))
  }

  private def fieldValueWithIdsFor(field: DataTypeField, fieldValue: JsValue): DBIO[JsValue] = {
    field.fieldType match {
      case t: BuiltInType => DBIO.successful(t.prepareJsValue(fieldValue))
      case t: BehaviorBackedDataType => {
        fieldsForBehaviorIdAction(t.behaviorVersion.behavior.id).flatMap { fields =>
          dataWithFieldIdsFor(fieldValue, fields)
        }
      }
    }
  }

  private def dataWithFieldIdsFor(data: JsValue, fields: Seq[DataTypeField]): DBIO[JsValue] = {
    data match {
      case obj: JsObject => {
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

  private def saveItemForBehaviorAction(maybeExistingId: Option[String], behavior: Behavior, user: User, data: JsValue): DBIO[DefaultStorageItem] = {
    val idToUse = maybeExistingId.getOrElse(IDs.next)
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
          saveItemAction(field.fieldType.name, user, fieldData, behavior.group).map { maybeItem =>
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
      fields <- fieldsForBehaviorIdAction(behavior.id)
      newDataWithFieldIds <- dataWithFieldIdsFor(newData, fields)
      newInstanceToSave <- DBIO.successful(DefaultStorageItem(idToUse, behavior, OffsetDateTime.now, user.id, newDataWithFieldIds))
      _ <- DBIO.successful(println(s"saving $newInstanceToSave"))
      _ <- maybeExistingId.map { existingId =>
        rawFindQueryFor(existingId).update(newInstanceToSave.toRaw)
      }.getOrElse {
        (all += newInstanceToSave.toRaw)
      }
      newInstanceToReturn <- tuple2Item((newInstanceToSave.toRaw, ((behavior.toRaw, team), Some((group.toRaw, team)))))
    } yield newInstanceToReturn
  }

  private def maybeExistingIdFor(data: JsValue): Option[String] = (data \ "id").asOpt[String]

  def saveItemAction(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): DBIO[DefaultStorageItem] = {
    val maybeExistingId = (data \ "id").asOpt[String]
    ((for {
      maybeBehavior <- dataService.behaviors.findByNameAction(typeName, behaviorGroup)
      maybeItem <- maybeBehavior.map { behavior =>
        saveItemForBehaviorAction(maybeExistingId, behavior, user, data).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeItem) transactionally).map { maybeNewItem =>
      maybeNewItem.getOrElse {
        throw new CreationTypeNotFoundException(typeName)
      }
    }
  }

  def createItemForBehavior(behavior: Behavior, user: User, data: JsValue): Future[DefaultStorageItem] = {
    dataService.run(saveItemForBehaviorAction(None, behavior, user, data))
  }

  def createItem(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    maybeExistingIdFor(data).map { existingId =>
      throw new IdPassedForCreationException(existingId)
    }.getOrElse {
      val action = saveItemAction(typeName, user, data, behaviorGroup)
      dataService.run(action)
    }
  }

  def updateItem(typeName: String, user: User, data: JsValue, behaviorGroup: BehaviorGroup): Future[DefaultStorageItem] = {
    if (maybeExistingIdFor(data).isEmpty) {
      throw new NoIdPassedForUpdateException()
    } else {
      val action = saveItemAction(typeName, user, data, behaviorGroup)
      dataService.run(action)
    }
  }

  def deleteItemAction(id: String, behaviorGroup: BehaviorGroup): DBIO[Option[DefaultStorageItem]] = {
    for {
      maybeItem <- findByIdAction(id, behaviorGroup)
      _ <- maybeItem.map(_ => rawFindQueryFor(id).delete).getOrElse(DBIO.successful({}))
    } yield maybeItem
  }

  def deleteItem(id: String, behaviorGroup: BehaviorGroup): Future[Option[DefaultStorageItem]] = {
    dataService.run(deleteItemAction(id, behaviorGroup))
  }

  def deleteItems(ids: Seq[String], behaviorGroup: BehaviorGroup): Future[Int] = {
    Future.sequence(ids.map(ea => deleteItem(ea, behaviorGroup))).map { deletedItemOptions =>
      deletedItemOptions.flatten.length
    }
  }

  def deleteFilteredItemsForBehavior(behavior: Behavior, filter: JsValue): Future[Seq[DefaultStorageItem]] = {
    val action = for {
      found <- filterForBehaviorAction(behavior, filter)
      _ <- DBIO.sequence(found.map { ea =>
        deleteItemAction(ea.id, behavior.group)
      })
    } yield found
    dataService.run(action)
  }

  def deleteFilteredItemsFor(typeName: String, filter: JsValue, behaviorGroup: BehaviorGroup): Future[Seq[DefaultStorageItem]] = {
    dataService.behaviors.findByIdOrName(typeName, behaviorGroup).flatMap { maybeBehavior =>
      maybeBehavior.map { behavior =>
        deleteFilteredItemsForBehavior(behavior, filter)
      }.getOrElse(Future.successful(Seq()))
    }
  }

}
