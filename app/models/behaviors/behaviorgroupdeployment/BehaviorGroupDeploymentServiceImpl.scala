package models.behaviors.behaviorgroupdeployment

import java.time.OffsetDateTime

import javax.inject.Inject
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.events.Event
import models.behaviors.triggers.Trigger
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

  def mostRecentForTeam(team: Team): Future[Seq[BehaviorGroupDeployment]] = {
    dataService.run(mostRecentForTeamQuery(team.id).result)
  }

  def maybeActiveBehaviorGroupVersionFor(group: BehaviorGroup, context: String, channel: String): Future[Option[BehaviorGroupVersion]] = {
    for {
      maybeDevModeChannel <- dataService.devModeChannels.find(context, channel, group.team)
      maybeGroupVersion <- if (maybeDevModeChannel.nonEmpty) {
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      } else {
        for {
          maybeDeployment <- maybeMostRecentFor(group)
          maybeVersion <- maybeDeployment.map { ea =>
            dataService.behaviorGroupVersions.findWithoutAccessCheck(ea.groupVersionId)
          }.getOrElse(Future.successful(None))
        } yield maybeVersion
      }
    } yield maybeGroupVersion
  }

  def allActiveTriggersFor(context: String, channel: String, team: Team): Future[Seq[Trigger]] = {
    for {
      maybeDevModeChannel <- dataService.devModeChannels.find(context, channel, team)
      triggers <- if (maybeDevModeChannel.nonEmpty) {
        dataService.triggers.allActiveFor(team)
      } else {
        for {
          deployments <- mostRecentForTeam(team)
          groupVersions <- Future.sequence(deployments.map { ea =>
            dataService.behaviorGroupVersions.findWithoutAccessCheck(ea.groupVersionId)
          }).map(_.flatten)
          behaviorVersions <- Future.sequence(groupVersions.map { ea =>
            dataService.behaviorVersions.allForGroupVersion(ea)
          }).map(_.flatten)
          triggers <- Future.sequence(behaviorVersions.map { ea =>
            dataService.triggers.allFor(ea)
          }).map(_.flatten)
        } yield triggers
      }
    } yield triggers
  }

  def possibleActivatedTriggersFor(
                                     event: Event,
                                     maybeTeam: Option[Team],
                                     maybeChannel: Option[String],
                                     context: String,
                                     maybeLimitToBehavior: Option[Behavior]
                                   ): Future[Seq[Trigger]] = {
    for {
      maybeLimitToBehaviorVersion <- maybeLimitToBehavior.map { limitToBehavior =>
        dataService.behaviors.maybeCurrentVersionFor(limitToBehavior)
      }.getOrElse(Future.successful(None))
      triggers <- maybeLimitToBehaviorVersion.map { limitToBehaviorVersion =>
        dataService.triggers.allFor(limitToBehaviorVersion)
      }.getOrElse {
        (for {
          team <- maybeTeam
          channel <- maybeChannel
        } yield {
          dataService.behaviorGroupDeployments.allActiveTriggersFor(context, channel, team)
        }).getOrElse(Future.successful(Seq()))
      }
    } yield triggers
  }

  def maybeMostRecentFor(group: BehaviorGroup): Future[Option[BehaviorGroupDeployment]] = {
    val action = mostRecentForBehaviorGroupQuery(group.id).result.map { r =>
      r.headOption
    }
    dataService.run(action)
  }

  def findForBehaviorGroupVersionIdAction(groupVersionId: String): DBIO[Option[BehaviorGroupDeployment]] = {
    findForBehaviorGroupVersionQuery(groupVersionId).result.map(_.headOption)
  }

  def findForBehaviorGroupVersionAction(version: BehaviorGroupVersion): DBIO[Option[BehaviorGroupDeployment]] = {
    findForBehaviorGroupVersionIdAction(version.id)
  }

  def findForBehaviorGroupVersionId(groupVersionId: String): Future[Option[BehaviorGroupDeployment]] = {
    dataService.run(findForBehaviorGroupVersionIdAction(groupVersionId))
  }

  def findForBehaviorGroupVersion(version: BehaviorGroupVersion): Future[Option[BehaviorGroupDeployment]] = {
    dataService.run(findForBehaviorGroupVersionAction(version))
  }

  def mostRecentBehaviorGroupVersionIds: Future[Seq[String]] = {
    dataService.run(mostRecentBehaviorGroupVersionIdsQuery.result)
  }

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment] = {
    val action = for {
      maybeExisting <- findForBehaviorGroupVersionQuery(version.id).result.map(r => r.headOption)
      instance <- maybeExisting.map(DBIO.successful).getOrElse {
        val newInstance = BehaviorGroupDeployment(IDs.next, version.group.id, version.id, maybeComment, userId, OffsetDateTime.now)
        (insertQuery += newInstance).map(_ => newInstance)
      }
    } yield instance
    dataService.run(action.transactionally)
  }

  def hasUndeployedVersionForAuthorAction(version: BehaviorGroupVersion, user: User): DBIO[Boolean] = {
    for {
      isDeployed <- findForBehaviorGroupVersionAction(version).map(_.isDefined)
      hasNewerForAuthor <- dataService.behaviorGroupVersions.hasNewerVersionForAuthorAction(version, user)
    } yield {
      isDeployed && hasNewerForAuthor
    }
  }

}
