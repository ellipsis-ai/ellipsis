package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorGroupData
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorGroupVersion(
                                   id: String,
                                   groupId: String,
                                   name: String,
                                   maybeIcon: Option[String],
                                   maybeDescription: Option[String],
                                   maybeAuthorId: Option[String],
                                   createdAt: OffsetDateTime
                                 )

class BehaviorGroupVersionsTable(tag: Tag) extends Table[RawBehaviorGroupVersion](tag, "behavior_group_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")
  def name = column[String]("name")
  def maybeIcon = column[Option[String]]("icon")
  def maybeDescription = column[Option[String]]("description")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, groupId, name, maybeIcon, maybeDescription, maybeAuthorId, createdAt) <>
      ((RawBehaviorGroupVersion.apply _).tupled, RawBehaviorGroupVersion.unapply _)
}

class BehaviorGroupVersionServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends BehaviorGroupVersionService {

  def dataService = dataServiceProvider.get

  import BehaviorGroupVersionQueries._

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2BehaviorGroupVersion))
    dataService.run(action)
  }

  def allFor(group: BehaviorGroup): Future[Seq[BehaviorGroupVersion]] = {
    val action = allForQuery(group.id).result.map { r =>
      r.map(tuple2BehaviorGroupVersion)
    }
    dataService.run(action)
  }

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None
               ): Future[BehaviorGroupVersion] = {
    val raw = RawBehaviorGroupVersion(IDs.next, group.id, maybeName.getOrElse(""), maybeIcon, maybeDescription, Some(user.id), OffsetDateTime.now)

    val action = (all += raw).map { _ =>
      BehaviorGroupVersion(raw.id, group, raw.name, raw.maybeIcon, raw.maybeDescription, Some(user), raw.createdAt)
    }
    dataService.run(action)
  }

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 data: BehaviorGroupData
               ): Future[BehaviorGroupVersion] = {
    for {
      groupVersion <- createFor(group, user, data.name, data.icon, data.description)
      _ <- Future.sequence(data.behaviorVersions.map { ea =>
        ea.behaviorId.map { behaviorId =>
          for {
            maybeExistingBehavior <- dataService.behaviors.find(behaviorId, user)
            behavior <- maybeExistingBehavior.map(Future.successful).getOrElse {
              dataService.behaviors.createFor(group, Some(behaviorId), None, ea.config.dataTypeName)
            }
            behaviorVersion <- dataService.behaviorVersions.createFor(behavior, groupVersion, Some(user), ea)
          } yield Some(behaviorVersion)
        }.getOrElse(Future.successful(None))
      })
    } yield groupVersion
  }

}
