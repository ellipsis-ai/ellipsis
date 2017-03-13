package models.behaviors.behavior

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.{AWSLambdaService, DataService}
import drivers.SlickPostgresDriver.api._
import models.behaviors.events.SlackMessageEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class RawBehavior(
                        id: String,
                        teamId: String,
                        groupId: Option[String],
                        maybeExportId: Option[String],
                        isDataType: Boolean,
                        createdAt: OffsetDateTime
                      )

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def groupId = column[Option[String]]("group_id")
  def maybeExportId = column[Option[String]]("export_id")
  def isDataType = column[Boolean]("is_data_type")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, teamId, groupId, maybeExportId, isDataType, createdAt) <>
    ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

class BehaviorServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService],
                                      lambdaServiceProvider: Provider[AWSLambdaService]
                                    ) extends BehaviorService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorQueries._

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2Behavior))
    dataService.run(action)
  }

  def findByName(name: String, group: BehaviorGroup): Future[Option[Behavior]] = {
    dataService.behaviorVersions.findCurrentByName(name, group).map { maybeBehaviorVersion =>
      maybeBehaviorVersion.map(_.behavior)
    }
  }

  def findByTrigger(trigger: String, group: BehaviorGroup): Future[Option[Behavior]] = {
    for {
      triggers <- dataService.messageTriggers.allActiveFor(group)
    } yield {
      val activated = triggers.filter(_.matches(trigger, includesBotMention = true))
      activated.map(_.behaviorVersion.behavior).headOption
    }
  }

  def findByIdOrName(idOrName: String, group: BehaviorGroup): Future[Option[Behavior]] = {
    findWithoutAccessCheck(idOrName).flatMap { maybeById =>
      maybeById.map { b =>
        if (b.maybeGroup.contains(group)) {
          Future.successful(Some(b))
        } else {
          Future.successful(None)
        }
      }.getOrElse {
        findByName(idOrName, group)
      }
    }
  }

  def findByIdOrNameOrTrigger(idOrNameOrTrigger: String, group: BehaviorGroup): Future[Option[Behavior]] = {
    findByIdOrName(idOrNameOrTrigger, group).flatMap { maybeByName =>
      maybeByName.map(b => Future.successful(Some(b))).getOrElse {
        findByTrigger(idOrNameOrTrigger, group)
      }
    }
  }

  def find(id: String, user: User): Future[Option[Behavior]] = {
    for {
      maybeBehavior <- findWithoutAccessCheck(id)
      canAccess <- maybeBehavior.map { behavior =>
        dataService.users.canAccess(user, behavior)
      }.getOrElse(Future.successful(false))
    } yield {
      if (canAccess) {
        maybeBehavior
      } else {
        None
      }
    }
  }

  def allForTeam(team: Team): Future[Seq[Behavior]] = {
    val action = allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
    dataService.run(action)
  }

  def allForGroupAction(group: BehaviorGroup): DBIO[Seq[Behavior]] = {
    allForGroupQuery(group.id).result.map(_.map(tuple2Behavior))
  }

  def allForGroup(group: BehaviorGroup): Future[Seq[Behavior]] = {
    dataService.run(allForGroupAction(group))
  }

  def createFor(group: BehaviorGroup, maybeIdToUse: Option[String], maybeExportId: Option[String], isDataType: Boolean): Future[Behavior] = {
    val raw = RawBehavior(maybeIdToUse.getOrElse(IDs.next), group.team.id, Some(group.id), maybeExportId, isDataType, OffsetDateTime.now)

    val action = (all += raw).map { _ =>
      Behavior(raw.id, group.team, Some(group), raw.maybeExportId, raw.isDataType, raw.createdAt)
    }

    dataService.run(action)
  }

  def createFor(team: Team, maybeIdToUse: Option[String], maybeExportId: Option[String], isDataType: Boolean): Future[Behavior] = {
    for {
      group <- dataService.behaviorGroups.createFor(maybeExportId, team)
      behavior <- createFor(group, maybeIdToUse, maybeExportId, isDataType)
    } yield behavior
  }

  def delete(behavior: Behavior): Future[Behavior] = {
    dataService.run(findRawQueryFor(behavior.id).delete.map(_ => behavior))
  }

  def maybeCurrentVersionForAction(behavior: Behavior): DBIO[Option[BehaviorVersion]] = {
    for {
      maybeCurrentGroupVersion <- behavior.group.maybeCurrentVersionId.map { versionId =>
        dataService.behaviorGroupVersions.findWithoutAccessCheckAction(versionId)
      }.getOrElse(DBIO.successful(None))
      maybeCurrentBehaviorVersion <- maybeCurrentGroupVersion.map { groupVersion =>
        dataService.behaviorVersions.findForAction(behavior, groupVersion)
      }.getOrElse(DBIO.successful(None))
    } yield maybeCurrentBehaviorVersion
  }

  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]] = {
    dataService.run(maybeCurrentVersionForAction(behavior))
  }

  def unlearn(behavior: Behavior): Future[Unit] = {
    for {
      versions <- dataService.behaviorVersions.allFor(behavior)
      _ <- Future.sequence(versions.map(v => dataService.behaviorVersions.unlearn(v)))
      _ <- delete(behavior)
    } yield {}
  }

  def authorNamesFor(behavior: Behavior, event: SlackMessageEvent): Future[Seq[String]] = {
    for {
      versions <- dataService.behaviorVersions.allFor(behavior)
      authors <- Future.successful(versions.flatMap(_.maybeAuthor).distinct)
      authorNames <- Future.sequence(authors.map { ea =>
        dataService.users.maybeNameFor(ea, event)
      }).map(_.flatten)
    } yield authorNames
  }

  def ensureExportIdFor(behavior: Behavior): Future[Behavior] = {
    if (behavior.maybeExportId.isDefined) {
      Future.successful(behavior)
    } else {
      val newExportId = Some(IDs.next)
      val action = uncompiledFindRawQuery(behavior.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => behavior.copy(maybeExportId = newExportId))
    }
  }

}
