package models.behaviors.library

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait LibraryVersionService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[LibraryVersion]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[LibraryVersion]]

}
