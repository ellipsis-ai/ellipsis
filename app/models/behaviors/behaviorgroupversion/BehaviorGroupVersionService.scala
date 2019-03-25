package models.behaviors.behaviorgroupversion

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupVersionService {

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroupVersion]]

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]]

  def batchFor(group: BehaviorGroup, batchSize: Int = 20, offset: Int = 0): Future[Seq[BehaviorGroupVersion]]

  def maybeCurrentForAction(group: BehaviorGroup): DBIO[Option[BehaviorGroupVersion]]

  def maybeCurrentFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]]

  def allCurrentIds: Future[Seq[String]]

  def maybeFirstForAction(group: BehaviorGroup): DBIO[Option[BehaviorGroupVersion]]

  def maybeFirstFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]]

  def createForAction(
                       group: BehaviorGroup,
                       user: User,
                       maybeName: Option[String] = None,
                       maybeIcon: Option[String] = None,
                       maybeDescription: Option[String] = None,
                       maybeGitSHA: Option[String] = None
                     ): DBIO[BehaviorGroupVersion]

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None,
                 maybeGitSHA: Option[String] = None
               ): Future[BehaviorGroupVersion]

  def createForBehaviorGroupData(
                                  group: BehaviorGroup,
                                  user: User,
                                  data: BehaviorGroupData,
                                  forceNode6: Boolean
                                ): Future[BehaviorGroupVersion]

  def redeploy(groupVersion: BehaviorGroupVersion): Future[Unit]

  def redeployAllCurrentVersions: Future[Unit]

  def haveActionsWithNameAndSameInterface(
                                           actionName: String,
                                           groupVersion1: BehaviorGroupVersion,
                                           groupVersion2: BehaviorGroupVersion
                                         ): Future[Boolean]

  def isActive(groupVersion: BehaviorGroupVersion, context: String, channel: String): Future[Boolean]

  def activeFunctionNames: Future[Seq[String]]

  def hasNewerVersionForAuthorAction(version: BehaviorGroupVersion, user: User): DBIO[Boolean]

}
