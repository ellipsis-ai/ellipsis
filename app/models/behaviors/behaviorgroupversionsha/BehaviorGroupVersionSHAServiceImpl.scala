package models.behaviors.behaviorgroupversionsha

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class BehaviorGroupVersionSHAsTable(tag: Tag) extends Table[BehaviorGroupVersionSHA](tag, "behavior_group_version_shas") {

  def groupVersionId = column[String]("group_version_id", O.PrimaryKey)
  def gitSHA = column[String]("git_sha")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (groupVersionId, gitSHA, createdAt) <> ((BehaviorGroupVersionSHA.apply _).tupled, BehaviorGroupVersionSHA.unapply _)
}

class BehaviorGroupVersionSHAServiceImpl @Inject() (
                                                  dataServiceProvider: Provider[DataService],
                                                  implicit val ec: ExecutionContext
                                                ) extends BehaviorGroupVersionSHAService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[BehaviorGroupVersionSHAsTable]

  def findForActionId(groupVersionId: String): DBIO[Option[BehaviorGroupVersionSHA]] = {
    all.filter(_.groupVersionId === groupVersionId).result.headOption
  }

  def findForAction(groupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorGroupVersionSHA]] = {
    findForActionId(groupVersion.id)
  }

  def findForId(groupVersionId: String): Future[Option[BehaviorGroupVersionSHA]] = {
    dataService.run(findForActionId(groupVersionId))
  }

  def createForAction(groupVersion: BehaviorGroupVersion, gitSHA: String): DBIO[BehaviorGroupVersionSHA] = {
    for {
      maybeExisting <- findForAction(groupVersion)
      instance <- maybeExisting.map(DBIO.successful).getOrElse {
        val newInstance = BehaviorGroupVersionSHA(groupVersion.id, gitSHA, OffsetDateTime.now)
        (all += newInstance).map(_ => newInstance)
      }
    } yield instance
  }

  def createFor(groupVersion: BehaviorGroupVersion, gitSHA: String): Future[BehaviorGroupVersionSHA] = {
    dataService.run(createForAction(groupVersion, gitSHA))
  }

  def maybeCreateFor(group: BehaviorGroup, gitSHA: String): Future[Option[BehaviorGroupVersionSHA]] = {
    for {
      maybeCurrentVersion <- dataService.behaviorGroupVersions.maybeCurrentFor(group)
      maybeSHA <- maybeCurrentVersion.map { groupVersion =>
        createFor(groupVersion, gitSHA).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeSHA
  }

}
