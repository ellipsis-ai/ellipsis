package models.behaviors.behaviorgroup

import models.team.Team
import play.api.Configuration

import scala.concurrent.Future

trait BehaviorGroupService {

  def createFor(maybeName: Option[String], maybeIcon: Option[String], maybeDescription: Option[String], maybeExportId: Option[String], team: Team): Future[BehaviorGroup]

  def save(behaviorGroup: BehaviorGroup): Future[BehaviorGroup]

  def ensureExportIdFor(behaviorGroup: BehaviorGroup): Future[BehaviorGroup]

  def allFor(team: Team): Future[Seq[BehaviorGroup]]

  def find(id: String): Future[Option[BehaviorGroup]]

  def merge(groups: Seq[BehaviorGroup]): Future[BehaviorGroup]

  def delete(group: BehaviorGroup): Future[BehaviorGroup]

  def editLinkFor(groupId: String, configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(groupId)
    s"$baseUrl$path"
  }

}
