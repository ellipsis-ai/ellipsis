package models.behaviors.behaviorgroupdeployment

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.events.Event
import models.behaviors.triggers.Trigger
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupDeploymentService {

  def mostRecentForTeam(team: Team): Future[Seq[BehaviorGroupDeployment]]

  def maybeActiveBehaviorGroupVersionFor(group: BehaviorGroup, context: String, channel: String): Future[Option[BehaviorGroupVersion]]

  def allActiveTriggersFor(context: String, maybeChannel: Option[String], team: Team): Future[Seq[Trigger]]

  def possibleActivatedTriggersFor(
                             maybeTeam: Option[Team],
                             maybeChannel: Option[String],
                             context: String,
                             maybeLimitToBehavior: Option[Behavior]
                           ): Future[Seq[Trigger]]

  def maybeMostRecentFor(group: BehaviorGroup): Future[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersionAction(version: BehaviorGroupVersion): DBIO[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersionIdAction(groupVersionId: String): DBIO[Option[BehaviorGroupDeployment]]
  def findForBehaviorGroupVersionId(groupVersionId: String): Future[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersion(version: BehaviorGroupVersion): Future[Option[BehaviorGroupDeployment]]

  def mostRecentBehaviorGroupVersionIds: Future[Seq[String]]

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment]

  def hasUndeployedVersionForAuthorAction(version: BehaviorGroupVersion, user: User): DBIO[Boolean]

}
