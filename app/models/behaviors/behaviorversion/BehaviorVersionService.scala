package models.behaviors.behaviorversion

import json.BehaviorVersionData
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.events.Event
import models.behaviors.{BotResult, ParameterWithValue}
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BehaviorVersionService {

  def currentIdsWithFunction: Future[Seq[String]]

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]]

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[BehaviorVersion]]

  def allCurrentForTeam(team: Team): Future[Seq[BehaviorVersion]]

  def dataTypesForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[BehaviorVersion]] = {
    allForGroupVersion(groupVersion).map { all =>
      all.filter(_.isDataType)
    }
  }

  def dataTypesForTeam(team: Team): Future[Seq[BehaviorVersion]] = {
    allCurrentForTeam(team).map { all =>
      all.filter(_.isDataType)
    }
  }

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]]

  def findForAction(behavior: Behavior, groupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorVersion]]

  def findFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): Future[Option[BehaviorVersion]]

  def findCurrentByName(name: String, group: BehaviorGroup): Future[Option[BehaviorVersion]]

  def hasSearchParam(behaviorVersion: BehaviorVersion): Future[Boolean]

  def createFor(behavior: Behavior, groupVersion: BehaviorGroupVersion, maybeUser: Option[User], maybeId: Option[String]): Future[BehaviorVersion]

  def createFor(
                 behavior: Behavior,
                 groupVersion: BehaviorGroupVersion,
                 maybeUser: Option[User],
                 data: BehaviorVersionData
               ): Future[BehaviorVersion]

  def save(behaviorVersion: BehaviorVersion): Future[BehaviorVersion]

  def delete(behaviorVersion: BehaviorVersion): Future[BehaviorVersion]

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]]

  def maybePreviousFor(behaviorVersion: BehaviorVersion): Future[Option[BehaviorVersion]]

  def maybeNotReadyResultFor(behaviorVersion: BehaviorVersion, event: Event): Future[Option[BotResult]]

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: Event
               ): Future[BotResult]

  def unlearn(behaviorVersion: BehaviorVersion): Future[Unit]

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit]

}
