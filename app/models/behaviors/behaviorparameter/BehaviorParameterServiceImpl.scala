package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
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

  def allForAction(behaviorVersion: BehaviorVersion): DBIO[Seq[BehaviorParameter]] = {
    allForQuery(behaviorVersion.id).result.map(_.map(tuple2Parameter).sortBy(_.rank))
  }

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]] = {
    dataService.run(allForAction(behaviorVersion))
  }

  private def createForAction(input: Input, rank: Int, behaviorVersion: BehaviorVersion): DBIO[BehaviorParameter] = {
    val raw = RawBehaviorParameter(IDs.next, rank, Some(input.inputId), behaviorVersion.id)
    (all += raw).map { _ =>
      BehaviorParameter(raw.id, raw.rank, input, behaviorVersion)
    }
  }

  private def createFor(input: Input, rank: Int, behaviorVersion: BehaviorVersion): Future[BehaviorParameter] = {
    dataService.run(createForAction(input, rank, behaviorVersion))
  }

  def ensureForAction(behaviorVersion: BehaviorVersion, inputs: Seq[Input]): DBIO[Seq[BehaviorParameter]] = {
    for {
      _ <- all.filter(_.behaviorVersionId === behaviorVersion.id).delete
      newParams <- DBIO.sequence(inputs.zipWithIndex.map { case(input, i) =>
        createForAction(input, i + 1, behaviorVersion)
      })
    } yield newParams
  }

  def isFirstForBehaviorVersionAction(parameter: BehaviorParameter): DBIO[Boolean] = {
    allForAction(parameter.behaviorVersion).map { all =>
      all.headOption.contains(parameter)
    }
  }

}
