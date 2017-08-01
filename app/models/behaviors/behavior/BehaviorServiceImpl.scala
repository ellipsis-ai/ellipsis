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

  def findWithoutAccessCheckAction(id: String): DBIO[Option[Behavior]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Behavior))
  }

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]] = {
    dataService.run(findWithoutAccessCheckAction(id))
  }

  def findByName(name: String, group: BehaviorGroup): Future[Option[Behavior]] = {
    dataService.behaviorVersions.findCurrentByName(name, group).map { maybeBehaviorVersion =>
      maybeBehaviorVersion.map(_.behavior)
    }
  }

  def findByNameAction(name: String, group: BehaviorGroup): DBIO[Option[Behavior]] = {
    dataService.behaviorVersions.findCurrentByNameAction(name, group).map { maybeBehaviorVersion =>
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

  def findAction(id: String, user: User): DBIO[Option[Behavior]] = {
    for {
      maybeBehavior <- findWithoutAccessCheckAction(id)
      canAccess <- maybeBehavior.map { behavior =>
        dataService.users.canAccessAction(user, behavior)
      }.getOrElse(DBIO.successful(false))
    } yield {
      if (canAccess) {
        maybeBehavior
      } else {
        None
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

  def createForAction(group: BehaviorGroup, maybeIdToUse: Option[String], maybeExportId: Option[String], isDataType: Boolean): DBIO[Behavior] = {
    val raw = RawBehavior(maybeIdToUse.getOrElse(IDs.next), group.team.id, Some(group.id), maybeExportId.orElse(Some(IDs.next)), isDataType, OffsetDateTime.now)

    (all += raw).map { _ =>
      Behavior(raw.id, group.team, Some(group), raw.maybeExportId, raw.isDataType, raw.createdAt)
    }
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

}
