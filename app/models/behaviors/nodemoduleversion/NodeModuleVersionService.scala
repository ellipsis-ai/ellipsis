package models.behaviors.nodemoduleversion

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

import scala.concurrent.Future

trait NodeModuleVersionService {

  def findAction(name: String, groupVersion: BehaviorGroupVersion): DBIO[Option[NodeModuleVersion]]

  def ensureForAction(name: String, version: String, groupVersion: BehaviorGroupVersion): DBIO[NodeModuleVersion]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[NodeModuleVersion]]

}
