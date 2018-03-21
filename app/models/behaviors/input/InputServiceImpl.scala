package models.behaviors.input

import javax.inject.Inject

import com.google.inject.Provider
import json.InputData
import models.IDs
import models.behaviors.behaviorparameter.{BehaviorParameterType, TextType}
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

import scala.concurrent.{ExecutionContext, Future}

case class RawInput(
                     id: String,
                     inputId: String,
                     maybeExportId: Option[String],
                     name: String,
                     maybeQuestion: Option[String],
                     paramType: String,
                     isSavedForTeam: Boolean,
                     isSavedForUser: Boolean,
                     behaviorGroupVersionId: String
                   )

class InputsTable(tag: Tag) extends Table[RawInput](tag, "inputs") {

  def id = column[String]("id", O.PrimaryKey)
  def inputId = column[String]("input_id")
  def maybeExportId = column[Option[String]]("export_id")
  def name = column[String]("name")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")
  def isSavedForTeam = column[Boolean]("is_saved_for_team")
  def isSavedForUser = column[Boolean]("is_saved_for_user")
  def behaviorGroupVersionId = column[String]("group_version_id")

  def * =
    (id, inputId, maybeExportId, name, maybeQuestion, paramType, isSavedForTeam, isSavedForUser, behaviorGroupVersionId) <>
      ((RawInput.apply _).tupled, RawInput.unapply _)
}

class InputServiceImpl @Inject() (
                                   dataServiceProvider: Provider[DataService],
                                   implicit val ec: ExecutionContext
                                 ) extends InputService {

  def dataService = dataServiceProvider.get

  import InputQueries._

  def findByInputIdAction(inputId: String, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Option[Input]] = {
    findByInputIdQuery(inputId, behaviorGroupVersion.id).result.map { r =>
      r.headOption.map(tuple2Input)
    }
  }

  def findByInputId(inputId: String, behaviorGroupVersion: BehaviorGroupVersion): Future[Option[Input]] = {
    dataService.run(findByInputIdAction(inputId, behaviorGroupVersion))
  }

  def findAction(id: String): DBIO[Option[Input]] = {
    findQuery(id).result.map { r =>
      r.headOption.map(tuple2Input)
    }
  }

  private def maybeParamTypeForAction(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorParameterType]] = {
    (data.paramType.flatMap { paramTypeData =>
      paramTypeData.id.orElse(paramTypeData.exportId).map { id =>
        BehaviorParameterType.findAction(id, behaviorGroupVersion, dataService)
      }
    }.getOrElse(DBIO.successful(None)))
  }

  def createForAction(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Input] = {
    for {
      maybeParamType <- maybeParamTypeForAction(data, behaviorGroupVersion)
      raw <- DBIO.successful(RawInput(
        data.id.getOrElse(IDs.next),
        data.inputId.getOrElse(IDs.next),
        Some(data.exportId.getOrElse(IDs.next)),
        data.name,
        data.maybeNonEmptyQuestion,
        maybeParamType.map(_.id).getOrElse(TextType.id),
        data.isSavedForTeam,
        data.isSavedForUser,
        behaviorGroupVersion.id
      ))
      input <- (all += raw).map { _ =>
        Input(
          raw.id,
          raw.inputId,
          raw.maybeExportId,
          raw.name,
          raw.maybeQuestion,
          maybeParamType.getOrElse(TextType),
          raw.isSavedForTeam,
          raw.isSavedForUser,
          behaviorGroupVersion
        )
      }
    } yield input
  }

  def ensureForAction(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Input] = {
    for {
      maybeExisting <- data.id.map(findAction).getOrElse(DBIO.successful(None))
      maybeParamType <- maybeParamTypeForAction(data, behaviorGroupVersion)
      input <- maybeExisting.map { existing =>
        val updated = existing.copy(
          maybeExportId = Some(data.exportId.getOrElse(IDs.next)),
          name = data.name,
          maybeQuestion = data.maybeNonEmptyQuestion,
          paramType = maybeParamType.getOrElse(TextType),
          isSavedForTeam = data.isSavedForTeam,
          isSavedForUser = data.isSavedForUser,
          behaviorGroupVersion = behaviorGroupVersion
        )
        uncompiledFindRawQuery(existing.id).update(updated.toRaw).map { _ => updated }
      }.getOrElse(createForAction(data, behaviorGroupVersion))
    } yield input
  }

  def allForGroupVersionAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[Input]] = {
    allForGroupVersionQuery(groupVersion.id).result.map { r =>
      r.map(tuple2Input)
    }
  }

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[Input]] = {
    dataService.run(allForGroupVersionAction(groupVersion))
  }

}
