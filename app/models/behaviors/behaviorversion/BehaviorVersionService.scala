package models.behaviors.behaviorversion

import json.BehaviorVersionData
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, ParameterWithValue}
import models.team.Team
import services.ApiConfigInfo
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait BehaviorVersionService {

  def currentFunctionNames: Future[Seq[String]]

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]]

  def allForGroupVersionAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[BehaviorVersion]]

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[BehaviorVersion]]

  def allCurrentForTeam(team: Team): Future[Seq[BehaviorVersion]]

  def dataTypesForGroupVersionAction(groupVersion: BehaviorGroupVersion)(implicit ec: ExecutionContext): DBIO[Seq[BehaviorVersion]] = {
    allForGroupVersionAction(groupVersion).map { all =>
      all.filter(_.isDataType)
    }
  }

  def dataTypesForTeam(team: Team)(implicit ec: ExecutionContext): Future[Seq[BehaviorVersion]] = {
    allCurrentForTeam(team).map { all =>
      all.filter(_.isDataType)
    }
  }

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]]

  def findForAction(behavior: Behavior, groupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorVersion]]

  def findFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): Future[Option[BehaviorVersion]]

  def findCurrentByName(name: String, group: BehaviorGroup): Future[Option[BehaviorVersion]]

  def findCurrentByNameAction(name: String, group: BehaviorGroup): DBIO[Option[BehaviorVersion]]

  def hasSearchParamAction(behaviorVersion: BehaviorVersion): DBIO[Boolean]

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       apiConfigInfo: ApiConfigInfo,
                       maybeUser: Option[User],
                       data: BehaviorVersionData,
                       forceNodeModuleUpdate: Boolean
                     ): DBIO[BehaviorVersion]

  def delete(behaviorVersion: BehaviorVersion): Future[BehaviorVersion]

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]]

  def maybePreviousFor(behaviorVersion: BehaviorVersion): Future[Option[BehaviorVersion]]

  def maybeNotReadyResultFor(behaviorVersion: BehaviorVersion, event: Event): Future[Option[BotResult]]

  def resultForAction(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       event: Event,
                       maybeConversation: Option[Conversation]
                     ): DBIO[BotResult]

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: Event,
                 maybeConversation: Option[Conversation]
               ): Future[BotResult]

  def unlearn(behaviorVersion: BehaviorVersion): Future[Unit]

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit]

  def redeployAllCurrentVersions: Future[Unit]

}
