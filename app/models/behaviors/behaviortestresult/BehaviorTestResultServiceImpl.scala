package models.behaviors.behaviortestresult

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.behaviors.SuccessResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.testing.TestMessageEvent
import services.{AWSLambdaService, DataService}

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
                                                 lambdaServiceProvider: Provider[AWSLambdaService],
                                                 implicit val ec: ExecutionContext
                                               ) extends BehaviorTestResultService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorTestResultQueries._

  private def createForAction(behaviorVersion: BehaviorVersion)
                             (implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BehaviorTestResult] = {
    val author = behaviorVersion.groupVersion.maybeAuthor.get
    val event = TestMessageEvent(author, behaviorVersion.team, "", includesBotMention = true)
    DBIO.from(dataService.behaviorVersions.resultFor(behaviorVersion, Seq(), event, None)).flatMap { result =>
      val outputText = result.fullText ++ result.maybeLog.map(l => s"\n\n$l").getOrElse("")
      val newInstance = BehaviorTestResult(
        IDs.next,
        result.maybeBehaviorVersion.get.id,
        result match {
          case _: SuccessResult => true
          case _ => false
        },
        outputText,
        OffsetDateTime.now
      )
      (all += newInstance).map(_ => newInstance)
    }
  }

  def ensureFor(behaviorVersion: BehaviorVersion)
               (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BehaviorTestResult] = {
    val action = findByBehaviorVersionQuery(behaviorVersion.id).result.flatMap { r =>
      r.headOption.map(DBIO.successful).getOrElse {
        createForAction(behaviorVersion)
      }
    }
    dataService.run(action)
  }

}
