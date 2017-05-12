package models.behaviors.library

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.LibraryVersionData
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LibraryVersionsTable(tag: Tag) extends Table[LibraryVersion](tag, "library_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def libraryId = column[String]("library_id")
  def name = column[String]("name")
  def functionBody = column[String]("function_body")
  def behaviorGroupVersionId = column[String]("group_version_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, libraryId, name, functionBody, behaviorGroupVersionId, createdAt) <> ((LibraryVersion.apply _).tupled, LibraryVersion.unapply _)
}

class LibraryVersionServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends LibraryVersionService {

  def dataService = dataServiceProvider.get

  import LibraryVersionQueries._

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[LibraryVersion]] = {
    allForQuery(groupVersion.id).result
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[LibraryVersion]] = {
    dataService.run(allForAction(groupVersion))
  }

  def findByLibraryIdWithoutAccessCheck(libraryId: String): Future[Option[LibraryVersion]] = {
    val action = findByLibraryIdQuery(libraryId).result.map(_.headOption)
    dataService.run(action)
  }

  def findByLibraryId(libraryId: String, user: User): Future[Option[LibraryVersion]] = {
    for {
      maybeLibraryVersion <- findByLibraryIdWithoutAccessCheck(libraryId)
      maybeGroupVersion <- maybeLibraryVersion.map { libraryVersion =>
        dataService.behaviorGroupVersions.findWithoutAccessCheck(libraryVersion.behaviorGroupVersionId)
      }.getOrElse(Future.successful(None))
      canAccess <- maybeGroupVersion.map { groupVersion =>
        dataService.users.canAccess(user, groupVersion.team)
      }.getOrElse(Future.successful(false))
    } yield {
      if (canAccess) {
        maybeLibraryVersion
      } else {
        None
      }
    }
  }

  def findAction(id: String): DBIO[Option[LibraryVersion]] = {
    findQuery(id).result.map(_.headOption)
  }

  def createForAction(data: LibraryVersionData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[LibraryVersion] = {
    val libraryVersion = LibraryVersion(
      data.id,
      data.libraryId,
      data.name,
      data.functionBody,
      behaviorGroupVersion.id,
      OffsetDateTime.now
    )
    (all += libraryVersion).map(_ => libraryVersion)
  }

  def ensureForAction(data: LibraryVersionData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[LibraryVersion] = {
    for {
      maybeExisting <- findAction(data.id)
      libraryVersion <- maybeExisting.map { existing =>
        val updated = existing.copy(
          name = data.name,
          behaviorGroupVersionId = behaviorGroupVersion.id
        )
        uncompiledFindQuery(existing.id).update(updated).map { _ => updated }
      }.getOrElse(createForAction(data, behaviorGroupVersion))
    } yield libraryVersion
  }

}
