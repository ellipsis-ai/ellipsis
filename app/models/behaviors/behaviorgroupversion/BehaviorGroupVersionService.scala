package models.behaviors.behaviorgroupversion

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupVersionService {

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroupVersion]]

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]]

  def allForAction(group: BehaviorGroup): DBIO[Seq[BehaviorGroupVersion]]

  def allFor(group: BehaviorGroup): Future[Seq[BehaviorGroupVersion]]

  def createForAction(
                       group: BehaviorGroup,
                       user: User,
                       maybeName: Option[String] = None,
                       maybeIcon: Option[String] = None,
                       maybeDescription: Option[String] = None
                     ): DBIO[BehaviorGroupVersion]

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None
               ): Future[BehaviorGroupVersion]

  def createForAction(
                       group: BehaviorGroup,
                       user: User,
                       data: BehaviorGroupData
                     ): DBIO[BehaviorGroupVersion]

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 data: BehaviorGroupData
               ): Future[BehaviorGroupVersion]

  def maybePreviousFor(groupVersion: BehaviorGroupVersion): Future[Option[BehaviorGroupVersion]]

  def redeploy(groupVersion: BehaviorGroupVersion): Future[Unit]

  def redeployAllCurrentVersions: Future[Unit]

  def currentFunctionNames: Future[Seq[String]]

}
