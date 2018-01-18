package json

import java.time.OffsetDateTime

import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeployment
import services.DataService

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

  def fromDeployment(deployment: BehaviorGroupDeployment, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorGroupDeploymentData] = {
    for {
      maybeUser <- dataService.users.find(deployment.userId)
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse(Future.successful(None))
      maybeDeployer <- (for {
        user <- maybeUser
        team <- maybeTeam
      } yield {
        dataService.users.userDataFor(user, team).map(Some(_))
      }).getOrElse(Future.successful(None))
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
}
