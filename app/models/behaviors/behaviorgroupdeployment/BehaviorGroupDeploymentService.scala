package models.behaviors.behaviorgroupdeployment

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team

import scala.concurrent.Future

trait BehaviorGroupDeploymentService {

  def allForTeam(team: Team): Future[Seq[BehaviorGroupDeployment]]

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment]

}
