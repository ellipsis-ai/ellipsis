package models.behaviors.library

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.Future

class LibraryVersionsTable(tag: Tag) extends Table[LibraryVersion](tag, "library_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def code = column[String]("code")
  def behaviorGroupVersionId = column[String]("group_version_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, name, code, behaviorGroupVersionId, createdAt) <> ((LibraryVersion.apply _).tupled, LibraryVersion.unapply _)
}

class LibraryVersionServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends LibraryVersionService {

  def dataService = dataServiceProvider.get

  def all = TableQuery[LibraryVersionsTable]

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[LibraryVersion]] = {
    all.filter(_.behaviorGroupVersionId === groupVersion.id).result
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[LibraryVersion]] = {
    dataService.run(allForAction(groupVersion))
  }

}
