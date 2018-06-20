package models.behaviors.behaviortestresult

import java.time.OffsetDateTime

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class BehaviorTestResultsTable(tag: Tag) extends Table[BehaviorTestResult](tag, "behavior_test_results") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def isPass = column[Boolean]("is_pass")
  def output = column[String]("output")
  def runAt = column[OffsetDateTime]("run_at")

  def * =
    (id, behaviorVersionId, isPass, output, runAt) <>
      ((BehaviorTestResult.apply _).tupled, BehaviorTestResult.unapply _)
}

class BehaviorTestResultServiceImpl @Inject() (
                                                 dataServiceProvider: Provider[DataService],
                                                 implicit val ec: ExecutionContext
                                               ) extends BehaviorTestResultService {

  def dataService = dataServiceProvider.get

  import BehaviorTestResultQueries._

  override def allFor(behaviorGroupVersion: BehaviorGroupVersion): Future[Seq[BehaviorTestResult]] = {
    val action = allForQuery(behaviorGroupVersion.id).result
    dataService.run(action)
  }

}
