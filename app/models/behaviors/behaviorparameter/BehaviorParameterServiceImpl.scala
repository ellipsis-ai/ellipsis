package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import json.{BehaviorParameterData, InputData}
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.input.Input

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorParameter(
                                 id: String,
                                 rank: Int,
                                 inputId: Option[String],
                                 behaviorVersionId: String
                               )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def rank = column[Int]("rank")
  def inputId = column[Option[String]]("input_id")
  def behaviorVersionId = column[String]("behavior_version_id")

  def * =
    (id, rank, inputId, behaviorVersionId) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
}

class BehaviorParameterServiceImpl @Inject() (
                                              dataServiceProvider: Provider[DataService]
                                            ) extends BehaviorParameterService {

  def dataService = dataServiceProvider.get

  import BehaviorParameterQueries._

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]] = {
   val action = allForQuery(behaviorVersion.id).result.map(_.map(tuple2Parameter).sortBy(_.rank))
    dataService.run(action)
  }

  private def createFor(input: Input, rank: Int, behaviorVersion: BehaviorVersion): Future[BehaviorParameter] = {
    val action = for {
      raw <- DBIO.successful {
        RawBehaviorParameter(IDs.next, rank, Some(input.id), behaviorVersion.id)
      }
      param <- (all += raw).map { _ =>
        BehaviorParameter(raw.id, raw.rank, input, behaviorVersion)
      }
    } yield param
    dataService.run(action)
  }

  def ensureFor(behaviorVersion: BehaviorVersion, params: Seq[BehaviorParameterData]): Future[Seq[BehaviorParameter]] = {
    val action = for {
      _ <- all.filter(_.behaviorVersionId === behaviorVersion.id).delete
      newParams <- DBIO.sequence(params.zipWithIndex.map { case(data, i) =>
        DBIO.from(for {
          maybeExistingInput <- if (data.isShared) {
            data.inputId.map { inputId =>
              dataService.inputs.find(inputId)
            }.getOrElse(Future.successful(None))
          } else {
            Future.successful(None)
          }
          input <- maybeExistingInput.map { existing =>
            InputData.fromInput(existing, dataService).flatMap { inputData =>
              val updatedInputData = inputData.copy(
                name = data.name,
                paramType = data.paramType,
                question = data.question,
                isSavedForTeam = data.isSavedForTeam.exists(identity),
                isSavedForUser = data.isSavedForUser.exists(identity)
              )
              dataService.inputs.ensureFor(updatedInputData, behaviorVersion.group)
            }
          }.getOrElse {
            dataService.inputs.createFor(data.newInputData, behaviorVersion.group)
          }
          param <- createFor(input, i + 1, behaviorVersion)
          _ <- if (data.isShared) {
            Future.successful({})
          } else {
            dataService.savedAnswers.updateForInputId(data.inputId, input.id)
          }
        } yield param)
      })
    } yield newParams
    dataService.run(action)
  }

  def isFirstForBehaviorVersion(parameter: BehaviorParameter): Future[Boolean] = {
    allFor(parameter.behaviorVersion).map { all =>
      all.headOption.contains(parameter)
    }
  }

}
