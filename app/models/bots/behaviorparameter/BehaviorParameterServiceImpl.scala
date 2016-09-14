package models.bots.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.bots.behaviorversion.BehaviorVersion
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
                                 maybeParamType: Option[String]
                               )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def rank = column[Int]("rank")
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeQuestion = column[Option[String]]("question")
  def maybeParamType = column[Option[String]]("param_type")

  def * =
    (id, name, rank, behaviorVersionId, maybeQuestion, maybeParamType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
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

  private def createFor(name: String, maybeQuestion: Option[String], rank: Int, behaviorVersion: BehaviorVersion): Future[BehaviorParameter] = {
    val raw = RawBehaviorParameter(IDs.next, name, rank, behaviorVersion.id, maybeQuestion, None)
    val action = (all += raw).map { _ =>
      BehaviorParameter(raw.id, raw.name, raw.rank, behaviorVersion, raw.maybeQuestion, raw.maybeParamType)
    }
    dataService.run(action)
  }

  def ensureFor(behaviorVersion: BehaviorVersion, params: Seq[(String, Option[String])]): Future[Seq[BehaviorParameter]] = {
    val action = for {
      _ <- all.filter(_.behaviorVersionId === behaviorVersion.id).delete
      newParams <- DBIO.sequence(params.zipWithIndex.map { case((name, maybeQuestion), i) =>
        DBIO.from(createFor(name, maybeQuestion, i + 1, behaviorVersion))
      })
    } yield newParams
    dataService.run(action)
  }
}
