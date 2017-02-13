package models.behaviors.input

import javax.inject.Inject

import com.google.inject.Provider
import json.InputData
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorParameterType, TextType}
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.behavior.Behavior

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawInput(
                     id: String,
                     maybeExportId: Option[String],
                     name: String,
                     maybeQuestion: Option[String],
                     paramType: String,
                     isSavedForTeam: Boolean,
                     isSavedForUser: Boolean,
                     maybeBehaviorGroupId: Option[String]
                   )

class InputsTable(tag: Tag) extends Table[RawInput](tag, "inputs") {

  def id = column[String]("id", O.PrimaryKey)
  def maybeExportId = column[Option[String]]("export_id")
  def name = column[String]("name")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")
  def isSavedForTeam = column[Boolean]("is_saved_for_team")
  def isSavedForUser = column[Boolean]("is_saved_for_user")
  def maybeBehaviorGroupId = column[Option[String]]("group_id")

  def * =
    (id, maybeExportId, name, maybeQuestion, paramType, isSavedForTeam, isSavedForUser, maybeBehaviorGroupId) <> ((RawInput.apply _).tupled, RawInput.unapply _)
}

class InputServiceImpl @Inject() (
                                   dataServiceProvider: Provider[DataService]
                                 ) extends InputService {

  def dataService = dataServiceProvider.get

  import InputQueries._

  def find(id: String): Future[Option[Input]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2Input)
    }
    dataService.run(action)
  }

  private def maybeParamTypeFor(data: InputData, team: Team): Future[Option[BehaviorParameterType]] = {
    data.paramType.flatMap { paramTypeData =>
      paramTypeData.id.map { id =>
        BehaviorParameterType.find(id, team, dataService)
      }
    }.getOrElse(Future.successful(None))
  }

  def createFor(data: InputData, team: Team): Future[Input] = {
    data.groupId.map { gid =>
      dataService.behaviorGroups.find(gid)
    }.getOrElse(Future.successful(None)).flatMap { maybeGroup =>
      val action = for {
        maybeParamType <- DBIO.from(maybeParamTypeFor(data, team))
        raw <- DBIO.successful(RawInput(
          IDs.next,
          Some(data.exportId.getOrElse(IDs.next)),
          data.name,
          data.maybeNonEmptyQuestion,
          maybeParamType.map(_.id).getOrElse(TextType.id),
          data.isSavedForTeam,
          data.isSavedForUser,
          maybeGroup.map(_.id)
        ))
        input <- (all += raw).map { _ =>
          Input(
            raw.id,
            raw.maybeExportId,
            raw.name,
            raw.maybeQuestion,
            maybeParamType.getOrElse(TextType),
            raw.isSavedForTeam,
            raw.isSavedForUser,
            maybeGroup
          )
        }
      } yield input
      dataService.run(action)
    }
  }

  def ensureFor(data: InputData, team: Team): Future[Input] = {
    for {
      maybeGroup <- data.groupId.map { gid =>
        dataService.behaviorGroups.find(gid)
      }.getOrElse(Future.successful(None))
      maybeExisting <- maybeGroup.map { group =>
        data.id.map(find).getOrElse(Future.successful(None))
      }.getOrElse(Future.successful(None))
      maybeParamType <- maybeParamTypeFor(data, team)
      input <- maybeExisting.map { existing =>
        val raw = existing.copy(
          maybeExportId = Some(data.exportId.getOrElse(IDs.next)),
          name = data.name,
          maybeQuestion = data.maybeNonEmptyQuestion,
          paramType = maybeParamType.getOrElse(TextType),
          isSavedForTeam = data.isSavedForTeam,
          isSavedForUser = data.isSavedForUser,
          maybeBehaviorGroup = maybeGroup
        ).toRaw
        val action = uncompiledFindRawQuery(existing.id).update(raw).map { _ => existing }
        dataService.run(action)
      }.getOrElse(createFor(data, team))
    } yield input
  }

  def allForGroup(group: BehaviorGroup): Future[Seq[Input]] = {
    val action = allForGroupQuery(group.id).result.map { r =>
      r.map(tuple2Input)
    }
    dataService.run(action)
  }

  def withEnsuredExportId(input: Input): Future[Input] = {
    if (input.maybeExportId.isDefined) {
      Future.successful(input)
    } else {
      val newExportId = Some(IDs.next)
      val action = uncompiledFindRawQuery(input.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map { _ => input.copy(maybeExportId = newExportId) }
    }
  }

  def ensureExportIdsFor(behavior: Behavior): Future[Unit] = {
    for {
      maybeCurrentVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      params <- maybeCurrentVersion.map { version =>
        dataService.behaviorParameters.allFor(version)
      }.getOrElse(Future.successful(Seq()))
      _ <- Future.sequence(params.map { param =>
        withEnsuredExportId(param.input)
      })
    } yield {}
  }

}
