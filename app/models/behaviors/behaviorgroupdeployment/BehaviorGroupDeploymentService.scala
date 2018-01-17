package models.behaviors.behaviorgroupdeployment

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team

import scala.concurrent.Future

trait BehaviorGroupDeploymentService {

  def allForTeam(team: Team): Future[Seq[BehaviorGroupDeployment]]

  def allActiveTriggersFor(context: String, channel: String, team: Team): Future[Seq[MessageTrigger]]

  def maybeMostRecentFor(group: BehaviorGroup): Future[Option[BehaviorGroupDeployment]]

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment]

}
