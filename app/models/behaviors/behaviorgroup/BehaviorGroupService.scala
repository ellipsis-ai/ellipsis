package models.behaviors.behaviorgroup

import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import play.api.Configuration

import scala.concurrent.Future

trait BehaviorGroupService {

  def createFor(maybeExportId: Option[String], team: Team): Future[BehaviorGroup]

  def allFor(team: Team): Future[Seq[BehaviorGroup]]

  def allWithNoNameFor(team: Team): Future[Seq[BehaviorGroup]]

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroup]]

  def find(id: String, user: User): Future[Option[BehaviorGroup]]

  def findForInvocationToken(tokenId: String): Future[Option[BehaviorGroup]]

  def merge(groups: Seq[BehaviorGroup], user: User): Future[BehaviorGroup]

  def delete(group: BehaviorGroup): Future[BehaviorGroup]

  def editLinkFor(groupId: String, configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(groupId)
    s"$baseUrl$path"
  }

  def maybeCurrentVersionFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]]

}
