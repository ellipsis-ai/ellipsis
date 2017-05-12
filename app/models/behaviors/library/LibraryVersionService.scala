package models.behaviors.library

import json.LibraryVersionData
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait LibraryVersionService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[LibraryVersion]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[LibraryVersion]]

  def findByLibraryId(libraryId: String, user: User): Future[Option[LibraryVersion]]

  def ensureForAction(data: LibraryVersionData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[LibraryVersion]

}
