package models.behaviors.input

import javax.inject.Inject

import com.google.inject.Provider
import json.InputData
import models.IDs
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
                     paramType: String
                   )

class InputsTable(tag: Tag) extends Table[RawInput](tag, "inputs") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")

  def * =
    (id, name, maybeQuestion, paramType) <> ((RawInput.apply _).tupled, RawInput.unapply _)
}

class InputServiceImpl @Inject() (
                                   dataServiceProvider: Provider[DataService]
                                 ) extends InputService {

  def dataService = dataServiceProvider.get

  import InputQueries._

  def ensureFor(data: InputData, team: Team): Future[Input] = {
    val raw = RawInput(IDs.next, data.name, data.maybeNonEmptyQuestion, data.paramType.map(_.id).getOrElse(TextType.id))
    val action = for {
      maybeParamType <- DBIO.from(data.paramType.map { paramTypeData =>
        BehaviorParameterType.find(paramTypeData.id, team, dataService)
      }.getOrElse(Future.successful(None)))
      input <- (all += raw).map { _ =>
        Input(raw.id, raw.name, raw.maybeQuestion, maybeParamType.getOrElse(TextType))
      }
    } yield input
    dataService.run(action)
  }
}
