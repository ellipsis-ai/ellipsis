package models.behaviors.behavior

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.SlackMessageEvent
import models.team.Team
import play.api.Configuration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BehaviorService {

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]]

  def findByIdOrName(idOrName: String, group: BehaviorGroup): Future[Option[Behavior]]

  def findByIdOrNameOrTrigger(idOrNameOrTrigger: String, group: BehaviorGroup): Future[Option[Behavior]]

  def find(id: String, user: User): Future[Option[Behavior]]

  def allForTeam(team: Team): Future[Seq[Behavior]]

  def allForGroup(group: BehaviorGroup): Future[Seq[Behavior]]

  def regularForGroup(group: BehaviorGroup): Future[Seq[Behavior]] = {
    allForGroup(group).map { all =>
      all.filterNot(_.isDataType)
    }
  }

  def regularForTeam(team: Team): Future[Seq[Behavior]] = {
    allForTeam(team).map { all =>
      all.filter(_.maybeDataTypeName.isEmpty)
    }
  }

  def dataTypesForTeam(team: Team): Future[Seq[Behavior]] = {
    allForTeam(team).map { all =>
      all.filter(_.isDataType)
    }
  }

  def dataTypesForGroup(group: BehaviorGroup): Future[Seq[Behavior]] = {
    allForGroup(group).map { all =>
      all.filter(_.isDataType)
    }
  }

  def createFor(team: Team, maybeIdToUse: Option[String], maybeExportId: Option[String], maybeDataTypeName: Option[String]): Future[Behavior]

  def createFor(group: BehaviorGroup, maybeIdToUse: Option[String], maybeExportId: Option[String], maybeDataTypeName: Option[String]): Future[Behavior]

  def updateDataTypeNameFor(behavior: Behavior, maybeName: Option[String]): Future[Behavior]

  def hasSearchParam(behavior: Behavior): Future[Boolean]

  def delete(behavior: Behavior): Future[Behavior]

  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]]

  def unlearn(behavior: Behavior): Future[Unit]

  def authorNamesFor(behavior: Behavior, event: SlackMessageEvent): Future[Seq[String]]

  def editLinkFor(groupId: String, behaviorId: String, configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(groupId, Some(behaviorId))
    s"$baseUrl$path"
  }

  def ensureExportIdFor(behavior: Behavior): Future[Behavior]

}
