package models.behaviors.behaviorgroupdeployment

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupDeploymentService {

  def maybeActiveBehaviorGroupVersionFor(group: BehaviorGroup, context: String, channel: String): Future[Option[BehaviorGroupVersion]]

  def allActiveTriggersFor(context: String, channel: String, team: Team): Future[Seq[MessageTrigger]]

  def possibleActivatedTriggersFor(
                             event: Event,
                             maybeTeam: Option[Team],
                             maybeChannel: Option[String],
                             context: String,
                             maybeLimitToBehavior: Option[Behavior]
                           ): Future[Seq[MessageTrigger]]

  def maybeMostRecentFor(group: BehaviorGroup): Future[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersionAction(version: BehaviorGroupVersion): DBIO[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersionId(groupVersionId: String): Future[Option[BehaviorGroupDeployment]]

  def findForBehaviorGroupVersion(version: BehaviorGroupVersion): Future[Option[BehaviorGroupDeployment]]

  def mostRecentBehaviorGroupVersionIds: Future[Seq[String]]

  def deploy(version: BehaviorGroupVersion, userId: String, maybeComment: Option[String]): Future[BehaviorGroupDeployment]

  def hasUndeployedVersionForAuthorAction(version: BehaviorGroupVersion, user: User): DBIO[Boolean]

}
