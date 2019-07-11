package models.behaviors.behaviorgroup

import json.{BehaviorGroupData, BehaviorGroupDeploymentData}
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsValue
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait BehaviorGroupService {

  def createFor(maybeExportId: Option[String], team: Team): Future[BehaviorGroup]

  def allForAction(team: Team): DBIO[Seq[BehaviorGroup]]

  def allFor(team: Team): Future[Seq[BehaviorGroup]]

  def allWithNoNameFor(team: Team): Future[Seq[BehaviorGroup]]

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroup]]
  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroup]]

  def find(id: String, user: User): Future[Option[BehaviorGroup]]

  def findForInvocationToken(tokenId: String): Future[Option[BehaviorGroup]]

  def merge(groups: Seq[BehaviorGroup], user: User): Future[BehaviorGroup]

  def delete(group: BehaviorGroup): Future[BehaviorGroup]

  def editLinkFor(groupId: String, configuration: Configuration): String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val path = controllers.routes.BehaviorEditorController.edit(groupId)
    s"$baseUrl$path"
  }

  def maybeCurrentVersionFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]]

  def saveVersionFor(user: User, jsonString: String, isReinstall: Option[Boolean]): Future[Option[JsValue]]

  def deploy(behaviorGroupId: String, user: User): Future[Option[BehaviorGroupDeploymentData]]

  def maybeDataFor(
                    groupId: String,
                    user: User
                  )(implicit ec: ExecutionContext): Future[Option[BehaviorGroupData]]

}
