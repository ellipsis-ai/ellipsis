package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorParameterData
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import services.DataService

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
    for {
      _ <- dataService.run(all.filter(_.behaviorVersionId === behaviorVersion.id).delete)
      newParams <- Future.sequence(params.zipWithIndex.map { case(data, i) =>
        for {
          maybeExistingInput <- data.inputVersionId.map { inputId =>
            dataService.inputs.find(inputId)
          }.getOrElse(Future.successful(None))
          maybeParam <- maybeExistingInput.map { input =>
            createFor(input, i + 1, behaviorVersion).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeParam
      }).map(_.flatten)
    } yield newParams
  }

  def isFirstForBehaviorVersion(parameter: BehaviorParameter): Future[Boolean] = {
    allFor(parameter.behaviorVersion).map { all =>
      all.headOption.contains(parameter)
    }
  }

}
