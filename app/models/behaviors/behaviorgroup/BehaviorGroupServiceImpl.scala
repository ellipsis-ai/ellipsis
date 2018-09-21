package models.behaviors.behaviorgroup

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorGroupData
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import services.DataService
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}


case class RawBehaviorGroup(
                             id: String,
                             maybeExportId: Option[String],
                             teamId: String,
                             createdAt: OffsetDateTime,
                             maybeDeletedAt: Option[OffsetDateTime]
                           )

class BehaviorGroupsTable(tag: Tag) extends Table[RawBehaviorGroup](tag, "behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def maybeExportId = column[Option[String]]("export_id")
  def teamId = column[String]("team_id")
  def createdAt = column[OffsetDateTime]("created_at")
  def maybeDeletedAt = column[Option[OffsetDateTime]]("deleted_at")

  def * = (id, maybeExportId, teamId, createdAt, maybeDeletedAt) <> ((RawBehaviorGroup.apply _).tupled, RawBehaviorGroup.unapply _)
}

class BehaviorGroupServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService],
                                          cacheServiceProvider: Provider[CacheService],
                                          implicit val ec: ExecutionContext
                                        ) extends BehaviorGroupService {

  def dataService = dataServiceProvider.get
  def cacheService = cacheServiceProvider.get

  import BehaviorGroupQueries._

  def createFor(maybeExportId: Option[String], team: Team): Future[BehaviorGroup] = {
    val raw = RawBehaviorGroup(IDs.next, maybeExportId.orElse(Some(IDs.next)), team.id, OffsetDateTime.now, None)
    val action = (insertQuery += raw).map(_ => tuple2Group((raw, team)))
    dataService.run(action)
  }

  def allFor(team: Team): Future[Seq[BehaviorGroup]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Group)
    }
    dataService.run(action)
  }

  def allWithNoNameFor(team: Team): Future[Seq[BehaviorGroup]] = {
    for {
      allGroups <- allFor(team)
      allCurrentVersions <- Future.sequence(allGroups.map { ea =>
        dataService.behaviorGroups.maybeCurrentVersionFor(ea)
      }).map(_.flatten)
    } yield {
      allCurrentVersions.
        filter(_.name.isEmpty).
        map(_.group)
    }
  }

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroup]] = {
    findQuery(id).result.map { r =>
      r.headOption.map(tuple2Group)
    }
  }

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroup]] = {
    dataService.run(findWithoutAccessCheckAction(id))
  }

  def find(id: String, user: User): Future[Option[BehaviorGroup]] = {
    val action = for {
      maybeGroup <- findWithoutAccessCheckAction(id)
      canAccess <- maybeGroup.map { group =>
        dataService.users.canAccessAction(user, group)
      }.getOrElse(DBIO.successful(false))
    } yield {
      if (canAccess) {
        maybeGroup
      } else {
        None
      }
    }
    dataService.run(action)
  }

  def findForInvocationToken(tokenId: String): Future[Option[BehaviorGroup]] = {
    for {
      maybeToken <- dataService.invocationTokens.findNotExpired(tokenId)
      maybeUser <- dataService.users.findForInvocationToken(tokenId)
      maybeOriginatingBehaviorVersion <- (for {
        token <- maybeToken
        user <- maybeUser
      } yield {
        dataService.behaviorVersions.find(token.behaviorVersionId, user)
      }).getOrElse(Future.successful(None))
    } yield maybeOriginatingBehaviorVersion.map(_.group)
  }

  def merge(groups: Seq[BehaviorGroup], user: User): Future[BehaviorGroup] = {
    Future.sequence(groups.map { ea =>
      dataService.behaviorGroups.maybeCurrentVersionFor(ea)
    }).flatMap { versions =>
      mergeVersions(versions.flatten, user)
    }
  }

  private def mergeVersions(groupVersions: Seq[BehaviorGroupVersion], user: User): Future[BehaviorGroup] = {
    val firstGroupVersion = groupVersions.head
    val team = firstGroupVersion.team
    val mergedName = groupVersions.map(_.name).filter(_.trim.nonEmpty).mkString("-")
    val descriptions = groupVersions.flatMap(_.maybeDescription).filter(_.trim.nonEmpty)
    val mergedDescription = if (descriptions.isEmpty) { None } else { Some(descriptions.mkString("\n")) }
    val maybeIcon = groupVersions.find(_.maybeIcon.isDefined).flatMap(_.maybeIcon)

    for {
      groupsData <- Future.sequence(groupVersions.map { ea =>
        BehaviorGroupData.buildFor(ea, user, None, dataService, cacheService)
      })
      userData <- dataService.users.userDataFor(user, team)
      mergedData <- Future.successful({
        val actionInputs = groupsData.flatMap(_.actionInputs)
        val dataTypeInputs = groupsData.flatMap(_.dataTypeInputs)
        val behaviorVersions = groupsData.flatMap(_.behaviorVersions).map(_.copyWithNewBehaviorIdForMerge)
        val libraryVersions = groupsData.flatMap(_.libraryVersions)
        val requiredAWSConfigs = groupsData.flatMap(_.requiredAWSConfigs)
        val requiredOAuthApiConfigs = groupsData.flatMap(_.requiredOAuthApiConfigs)
        val requiredSimpleTokenApis = groupsData.flatMap(_.requiredSimpleTokenApis)
        val isManaged = groupsData.exists(_.isManaged)
        val maybeManagedContactData = groupsData.find(_.managedContact.isDefined).flatMap(_.managedContact)
        val maybeLinkedGithubRepo = groupsData.find(_.linkedGithubRepo.isDefined).flatMap(_.linkedGithubRepo)
        BehaviorGroupData(
          None,
          team.id,
          Some(mergedName),
          mergedDescription,
          maybeIcon,
          actionInputs,
          dataTypeInputs,
          behaviorVersions,
          libraryVersions,
          requiredAWSConfigs,
          requiredOAuthApiConfigs,
          requiredSimpleTokenApis,
          gitSHA = None,
          exportId = None,
          createdAt = None,
          Some(userData),
          deployment = None,
          metaData = None,
          isManaged,
          maybeManagedContactData,
          maybeLinkedGithubRepo
        )
      })
      _ <- Future.sequence(groupVersions.map { ea =>
        delete(ea.group)
      })
      mergedGroup <- createFor(mergedData.exportId, team)
      mergedGroupVersion <- dataService.behaviorGroupVersions.createFor(mergedGroup, user, mergedData.copyForNewVersionOf(mergedGroup))
    } yield mergedGroupVersion.group
  }

  def delete(group: BehaviorGroup): Future[BehaviorGroup] = {
    val action = deleteQuery(group.id).update(Some(OffsetDateTime.now))
    dataService.run(action).map(_ => group)
  }

  def maybeCurrentVersionFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]] = {
    dataService.behaviorGroupVersions.maybeCurrentFor(group)
  }

}
