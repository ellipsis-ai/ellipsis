package models.behaviors.input

import javax.inject.Inject

import com.google.inject.Provider
import json.InputData
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorParameterType, TextType}
import models.team.Team
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawInput(
                     id: String,
                     name: String,
                     maybeQuestion: Option[String],
                     paramType: String,
                     isSavedForTeam: Boolean,
                     isSavedForUser: Boolean,
                     maybeBehaviorGroupId: Option[String]
                   )

class InputsTable(tag: Tag) extends Table[RawInput](tag, "inputs") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")
  def isSavedForTeam = column[Boolean]("is_saved_for_team")
  def isSavedForUser = column[Boolean]("is_saved_for_user")
  def maybeBehaviorGroupId = column[Option[String]]("group_id")

  def * =
    (id, name, maybeQuestion, paramType, isSavedForTeam, isSavedForUser, maybeBehaviorGroupId) <> ((RawInput.apply _).tupled, RawInput.unapply _)
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

  private def createFor(data: InputData, team: Team): Future[Input] = {
    val raw =
      RawInput(
        IDs.next,
        data.name,
        data.maybeNonEmptyQuestion,
        data.paramType.map(_.id).getOrElse(TextType.id),
        data.isSavedForTeam,
        data.isSavedForUser,
        None
      )
    val action = for {
      maybeParamType <- DBIO.from(data.paramType.map { paramTypeData =>
        BehaviorParameterType.find(paramTypeData.id, team, dataService)
      }.getOrElse(Future.successful(None)))
      input <- (all += raw).map { _ =>
        Input(
          raw.id,
          raw.name,
          raw.maybeQuestion,
          maybeParamType.getOrElse(TextType),
          raw.isSavedForTeam,
          raw.isSavedForUser,
          None
        )
      }
    } yield input
    dataService.run(action)
  }

  def ensureFor(data: InputData, team: Team): Future[Input] = {
    (if (data.isShared) {
      data.id.map(find).getOrElse(Future.successful(None))
    } else {
      Future.successful(None)
    }).flatMap { maybeExisting =>
      maybeExisting.map(Future.successful).getOrElse(createFor(data, team))
    }
  }

  def allFor(group: BehaviorGroup): Future[Seq[Input]] = {
    val action = allForGroupQuery(group.id).result.map { r =>
      r.map(tuple2Input)
    }
    dataService.run(action)
  }

  def changeGroup(input: Input, newGroup: BehaviorGroup): Future[Input] = {
    val action = all.filter(_.id === input.id).map(_.maybeBehaviorGroupId).update(Some(newGroup.id))
    dataService.run(action).map { _ => input.copy(maybeBehaviorGroup = Some(newGroup)) }
  }

}
