package models.behaviors.library

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.LibraryVersionData
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class RawLibraryVersion(
                              id: String,
                              libraryId: String,
                              maybeExportId: Option[String],
                              name: String,
                              maybeDescription: Option[String],
                              functionBody: String,
                              behaviorGroupVersionId: String,
                              createdAt: OffsetDateTime
                            )

class LibraryVersionsTable(tag: Tag) extends Table[RawLibraryVersion](tag, "library_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def libraryId = column[String]("library_id")
  def maybeExportId = column[Option[String]]("export_id")
  def name = column[String]("name")
  def maybeDescription = column[Option[String]]("description")
  def functionBody = column[String]("function_body")
  def behaviorGroupVersionId = column[String]("group_version_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, libraryId, maybeExportId, name, maybeDescription, functionBody, behaviorGroupVersionId, createdAt) <>
    ((RawLibraryVersion.apply _).tupled, RawLibraryVersion.unapply _)
}

class LibraryVersionServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             implicit val ec: ExecutionContext
                                           ) extends LibraryVersionService {

  def dataService = dataServiceProvider.get

  import LibraryVersionQueries._

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[LibraryVersion]] = {
    allForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2LibraryVersion)
    }
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[LibraryVersion]] = {
    dataService.run(allForAction(groupVersion))
  }

  def findByLibraryIdWithoutAccessCheck(libraryId: String, groupVersion: BehaviorGroupVersion): Future[Option[LibraryVersion]] = {
    val action = findByLibraryIdQuery(libraryId, groupVersion.id).result.map(_.map(tuple2LibraryVersion).headOption)
    dataService.run(action)
  }

  def findByLibraryId(libraryId: String, groupVersion: BehaviorGroupVersion, user: User): Future[Option[LibraryVersion]] = {
    for {
      maybeLibraryVersion <- findByLibraryIdWithoutAccessCheck(libraryId, groupVersion)
      canAccess <- maybeLibraryVersion.map { libraryVersion =>
        dataService.users.canAccess(user, libraryVersion.groupVersion.team)
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
    findQuery(id).result.map(_.map(tuple2LibraryVersion).headOption)
  }

  def find(id: String): Future[Option[LibraryVersion]] = {
    dataService.run(findAction(id))
  }

  def findCurrentByLibraryId(libraryId: String): Future[Option[LibraryVersion]] = {
    val action = findCurrentForLibraryIdQuery(libraryId).result.map { r =>
      r.map(tuple2LibraryVersion).headOption
    }
    dataService.run(action)
  }

  def uncompiledAllWithSubstringInGroupsQuery(substring: Rep[String], behaviorGroupVersionIds: Seq[String]) = {
    allWithGroupVersion.filter {
      case (libraryVersion, _) => libraryVersion.functionBody.like(substring) &&
        libraryVersion.behaviorGroupVersionId.inSet(behaviorGroupVersionIds)
    }
  }

  def allWithSubstringInGroupVersions(substring: String, behaviorGroupVersionIds: Seq[String]): Future[Seq[LibraryVersion]] = {
    val action = uncompiledAllWithSubstringInGroupsQuery(s"%${substring}%", behaviorGroupVersionIds).result.map { r =>
      r.map(tuple2LibraryVersion)
    }
    dataService.run(action)
  }

  def createForAction(data: LibraryVersionData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[LibraryVersion] = {
    val raw = RawLibraryVersion(
      data.id.getOrElse(IDs.next),
      data.libraryId.getOrElse(IDs.next),
      data.exportId,
      data.name,
      data.description,
      data.functionBody,
      behaviorGroupVersion.id,
      OffsetDateTime.now
    )
    (all += raw).map(_ => LibraryVersion(
      raw.id,
      raw.libraryId,
      raw.maybeExportId,
      raw.name,
      raw.maybeDescription,
      raw.functionBody,
      behaviorGroupVersion,
      raw.createdAt
    ))
  }

  def ensureForAction(data: LibraryVersionData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[LibraryVersion] = {
    for {
      maybeExisting <- data.id.map(findAction).getOrElse(DBIO.successful(None))
      libraryVersion <- maybeExisting.map { existing =>
        val updated = existing.copy(
          name = data.name,
          maybeDescription = data.description,
          groupVersion = behaviorGroupVersion,
          functionBody = data.functionBody
        )
        uncompiledRawFindQuery(existing.id).update(updated.toRaw).map { _ => updated }
      }.getOrElse(createForAction(data, behaviorGroupVersion))
    } yield libraryVersion
  }

}
