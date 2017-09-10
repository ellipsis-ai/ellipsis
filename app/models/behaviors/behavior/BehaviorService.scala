package models.behaviors.behavior

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.SlackMessageEvent
import models.team.Team
import play.api.Configuration
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait BehaviorService {

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]]

  def findWithoutAccessCheckAction(id: String): DBIO[Option[Behavior]]

  def findByIdOrName(idOrName: String, group: BehaviorGroup): Future[Option[Behavior]]

  def findByNameAction(name: String, group: BehaviorGroup): DBIO[Option[Behavior]]

  def findByIdOrNameOrTrigger(idOrNameOrTrigger: String, group: BehaviorGroup): Future[Option[Behavior]]

  def findAction(id: String, user: User): DBIO[Option[Behavior]]

  def find(id: String, user: User): Future[Option[Behavior]]

  def allForTeam(team: Team): Future[Seq[Behavior]]

  def allForGroupAction(group: BehaviorGroup): DBIO[Seq[Behavior]]

  def allForGroup(group: BehaviorGroup): Future[Seq[Behavior]]

  def regularForGroup(group: BehaviorGroup)(implicit ec: ExecutionContext): Future[Seq[Behavior]] = {
    allForGroup(group).map { all =>
      all.filterNot(_.isDataType)
    }
  }

  def regularForTeam(team: Team)(implicit ec: ExecutionContext): Future[Seq[Behavior]] = {
    allForTeam(team).map { all =>
      all.filterNot(_.isDataType)
    }
  }

  def dataTypesForTeam(team: Team)(implicit ec: ExecutionContext): Future[Seq[Behavior]] = {
    allForTeam(team).map { all =>
      all.filter(_.isDataType)
    }
  }

  def dataTypesForGroup(group: BehaviorGroup)(implicit ec: ExecutionContext): Future[Seq[Behavior]] = {
    allForGroup(group).map { all =>
      all.filter(_.isDataType)
    }
  }

  def createForAction(group: BehaviorGroup, maybeIdToUse: Option[String], maybeExportId: Option[String], isDataType: Boolean): DBIO[Behavior]

  def delete(behavior: Behavior): Future[Behavior]

  def maybeCurrentVersionForAction(behavior: Behavior): DBIO[Option[BehaviorVersion]]

  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]]

  def unlearn(behavior: Behavior): Future[Unit]

  def authorNamesFor(behavior: Behavior, event: SlackMessageEvent): Future[Seq[String]]

  def editLinkFor(groupId: String, maybeBehaviorId: Option[String], configuration: Configuration): String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val path = controllers.routes.BehaviorEditorController.edit(groupId, maybeBehaviorId)
    s"$baseUrl$path"
  }

}
