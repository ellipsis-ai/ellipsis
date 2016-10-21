package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import json.{BehaviorParameterData, BehaviorParameterTypeData}
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorParameter(
                                 id: String,
                                 name: String,
                                 rank: Int,
                                 behaviorVersionId: String,
                                 maybeQuestion: Option[String],
                                 paramType: String
                               )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def rank = column[Int]("rank")
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeQuestion = column[Option[String]]("question")
  def paramType = column[String]("param_type")

  def * =
    (id, name, rank, behaviorVersionId, maybeQuestion, paramType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
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
      raw <- DBIO.successful {
        RawBehaviorParameter(IDs.next, name, rank, behaviorVersion.id, maybeQuestion, maybeParamType.map(_.id).getOrElse(TextType.id))
      }
      param <- (all += raw).map { _ =>
        BehaviorParameter(raw.id, raw.name, raw.rank, behaviorVersion, raw.maybeQuestion, maybeParamType.getOrElse(TextType))
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
