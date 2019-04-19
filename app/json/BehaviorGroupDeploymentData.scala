package json

import java.time.OffsetDateTime

import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeployment
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorGroupDeploymentData(
                                        id: String,
                                        groupId: String,
                                        groupVersionId: String,
                                        comment: Option[String],
                                        deployer: Option[UserData],
                                        createdAt: OffsetDateTime
                                     )

object BehaviorGroupDeploymentData {

  def fromDeploymentAction(deployment: BehaviorGroupDeployment, dataService: DataService)(implicit ec: ExecutionContext): DBIO[BehaviorGroupDeploymentData] = {
    for {
      maybeUser <- dataService.users.findAction(deployment.userId)
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.findAction(user.teamId)
      }.getOrElse(DBIO.successful(None))
      maybeDeployer <- (for {
        user <- maybeUser
        team <- maybeTeam
      } yield {
        dataService.users.userDataForAction(user, team).map(Some(_))
      }).getOrElse(DBIO.successful(None))
    } yield {
      BehaviorGroupDeploymentData(
        deployment.id,
        deployment.groupId,
        deployment.groupVersionId,
        deployment.maybeComment,
        maybeDeployer,
        deployment.createdAt
      )
    }
  }

  def fromDeployment(deployment: BehaviorGroupDeployment, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorGroupDeploymentData] = {
    dataService.run(fromDeploymentAction(deployment, dataService))
  }
}
