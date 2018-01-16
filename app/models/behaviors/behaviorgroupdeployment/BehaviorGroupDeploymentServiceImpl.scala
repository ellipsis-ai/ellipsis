package models.behaviors.behaviorgroupdeployment

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorGroupDeployment(
                                    id: String,
                                    groupId: String,
                                    groupVersionId: String,
                                    maybeComment: Option[String],
                                    userId: String,
                                    createdAt: OffsetDateTime
                                  )

class BehaviorGroupDeploymentsTable(tag: Tag) extends Table[BehaviorGroupDeployment](tag, "behavior_group_deployments") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")
  def groupVersionId = column[String]("group_version_id")
  def maybeComment = column[Option[String]]("comment")
  def userId = column[String]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, groupId, groupVersionId, maybeComment, userId, createdAt) <>
      ((BehaviorGroupDeployment.apply _).tupled, BehaviorGroupDeployment.unapply _)
}

class BehaviorGroupDeploymentServiceImpl @Inject() (
                                                  dataServiceProvider: Provider[DataService],
                                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                                  implicit val ec: ExecutionContext
                                                ) extends BehaviorGroupDeploymentService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorGroupDeploymentQueries._

  def allForTeam(team: Team): Future[Seq[BehaviorGroupDeployment]] = {
    dataService.run(allForTeamQuery(team.id).result)
  }

  def maybeMostRecentFor(group: BehaviorGroup): Future[Option[BehaviorGroupDeployment]] = {
    val action = mostRecentForBehaviorGroupQuery(group.id).result.map { r =>
      r.headOption
    }
    dataService.run(action)
  }

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment] = {
    val action = for {
      maybeExisting <- findForBehaviorGroupVersionQuery(version.id).result.map(r => r.headOption)
      instance <- maybeExisting.map(DBIO.successful).getOrElse {
        val newInstance = BehaviorGroupDeployment(IDs.next, version.group.id, version.id, maybeComment, userId, OffsetDateTime.now)
        (all += newInstance).map(_ => newInstance)
      }
    } yield instance
    dataService.run(action.transactionally)
  }

}
