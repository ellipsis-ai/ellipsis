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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class RawBehaviorGroup(
                             id: String,
                             maybeExportId: Option[String],
                             teamId: String,
                             maybeCurrentVersionId: Option[String],
                             createdAt: OffsetDateTime
                           )

class BehaviorGroupsTable(tag: Tag) extends Table[RawBehaviorGroup](tag, "behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def maybeExportId = column[Option[String]]("export_id")
  def teamId = column[String]("team_id")
  def maybeCurrentVersionId = column[Option[String]]("current_version_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, maybeExportId, teamId, maybeCurrentVersionId, createdAt) <> ((RawBehaviorGroup.apply _).tupled, RawBehaviorGroup.unapply _)
}

class BehaviorGroupServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService]
                                        ) extends BehaviorGroupService {

  def dataService = dataServiceProvider.get

  import BehaviorGroupQueries._

  def createFor(maybeExportId: Option[String], team: Team): Future[BehaviorGroup] = {
    val raw = RawBehaviorGroup(IDs.next, maybeExportId.orElse(Some(IDs.next)), team.id, None, OffsetDateTime.now)
    val action = (all += raw).map(_ => tuple2Group((raw, team)))
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
      maybeOriginatingBehavior <- (for {
        token <- maybeToken
        user <- maybeUser
      } yield {
        dataService.behaviors.find(token.behaviorId, user)
      }).getOrElse(Future.successful(None))
    } yield maybeOriginatingBehavior.map(_.group)
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
        BehaviorGroupData.buildFor(ea, user, dataService)
      })
      mergedData <- Future.successful({
        val actionInputs = groupsData.flatMap(_.actionInputs)
        val dataTypeInputs = groupsData.flatMap(_.dataTypeInputs)
        val behaviorVersions = groupsData.flatMap(_.behaviorVersions)
        val libraryVersions = groupsData.flatMap(_.libraryVersions)
        val requiredAWSConfigs = groupsData.flatMap(_.requiredAWSConfigs)
        val requiredOAuth2ApiConfigs = groupsData.flatMap(_.requiredOAuth2ApiConfigs)
        val requiredSimpleTokenApis = groupsData.flatMap(_.requiredSimpleTokenApis)
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
          nodeModuleVersions = Seq(),
          requiredAWSConfigs,
          requiredOAuth2ApiConfigs,
          requiredSimpleTokenApis,
          githubUrl = None,
          exportId = None, // Don't think it makes sense to have an exportId for something merged
          None
        )
      })
      _ <- Future.sequence(groupVersions.map { ea =>
        dataService.behaviorGroups.delete(ea.group)
      })
      mergedGroup <- dataService.behaviorGroups.createFor(mergedData.exportId, team)
      mergedGroupVersion <- dataService.behaviorGroupVersions.createFor(mergedGroup, user, mergedData.copyForNewVersionOf(mergedGroup), forceNodeModuleUpdate = false)
    } yield mergedGroupVersion.group
  }

  def delete(group: BehaviorGroup): Future[BehaviorGroup] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(group)
      _ <- Future.sequence(behaviors.map { ea =>
        dataService.behaviors.unlearn(ea)
      })
      deleted <- {
        val action = rawFindQuery(group.id).delete
        dataService.run(action).map(_ => group)
      }
    } yield deleted
  }

  def maybeCurrentVersionFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]] = {
    group.maybeCurrentVersionId.map { versionId =>
      dataService.behaviorGroupVersions.findWithoutAccessCheck(versionId)
    }.getOrElse(Future.successful(None))
  }

}
