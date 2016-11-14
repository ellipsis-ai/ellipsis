package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import json.{BehaviorParameterData, BehaviorParameterTypeData, InputData}
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorParameter(
                                 id: String,
                                 rank: Int,
                                 inputId: Option[String],
                                 behaviorVersionId: String,
                                 name: String,
                                 maybeQuestion: Option[String],
                                 paramType: String
                               )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def rank = column[Int]("rank")
  def inputId = column[Option[String]]("input_id")
  def behaviorVersionId = column[String]("behavior_version_id")
  def name = column[String]("name")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")

  def * =
    (id, rank, inputId, behaviorVersionId, name, maybeQuestion, paramType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
}

class BehaviorParameterServiceImpl @Inject() (
                                              dataServiceProvider: Provider[DataService]
                                            ) extends BehaviorParameterService {

  def dataService = dataServiceProvider.get

  import BehaviorParameterQueries._

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]] = {
   val action = allForQuery(behaviorVersion.id).result.map(_.map(tuple2Parameter))
    dataService.run(action)
  }

  private def createFor(name: String, paramTypeData: BehaviorParameterTypeData, maybeQuestion: Option[String], rank: Int, behaviorVersion: BehaviorVersion): Future[BehaviorParameter] = {
    val action = for {
      maybeParamType <- DBIO.from(BehaviorParameterType.find(paramTypeData.id, behaviorVersion.team, dataService))
      inputData <- DBIO.successful(InputData(name, Some(paramTypeData), maybeQuestion.getOrElse("")))
      input <- DBIO.from(dataService.inputs.ensureFor(inputData, behaviorVersion.team))
      raw <- DBIO.successful {
        RawBehaviorParameter(IDs.next, rank, Some(input.id), behaviorVersion.id, name, maybeQuestion, maybeParamType.map(_.id).getOrElse(TextType.id))
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
          paramTypeData <- data.paramType.map(Future.successful).getOrElse(BehaviorParameterTypeData.from(TextType, dataService))
          param <- createFor(data.name, paramTypeData, data.maybeNonEmptyQuestion, i + 1, behaviorVersion)
        } yield param)
      })
    } yield newParams
    dataService.run(action)
  }
}
