package models.behaviors.behaviorgroupversion

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import sangria.schema.Schema
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupVersionService {

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroupVersion]]

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]]

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

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 data: BehaviorGroupData,
                 forceNodeModuleUpdate: Boolean
               ): Future[BehaviorGroupVersion]

}
