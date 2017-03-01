package models.behaviors.behaviorgroup

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.input.{Input, InputQueries}
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class RawBehaviorGroup(
                             id: String,
                             name: String,
                             maybeIcon: Option[String],
                             maybeDescription: Option[String],
                             maybeExportId: Option[String],
                             teamId: String,
                             createdAt: OffsetDateTime
                           )

class BehaviorGroupsTable(tag: Tag) extends Table[RawBehaviorGroup](tag, "behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeIcon = column[Option[String]]("icon")
  def maybeDescription = column[Option[String]]("description")
  def maybeExportId = column[Option[String]]("export_id")
  def teamId = column[String]("team_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, name, maybeIcon, maybeDescription, maybeExportId, teamId, createdAt) <> ((RawBehaviorGroup.apply _).tupled, RawBehaviorGroup.unapply _)
}

class BehaviorGroupServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService]
                                        ) extends BehaviorGroupService {

  def dataService = dataServiceProvider.get

  import BehaviorGroupQueries._

  def createFor(name: String, maybeIcon: Option[String], description: String, maybeExportId: Option[String], team: Team): Future[BehaviorGroup] = {
    val raw = RawBehaviorGroup(IDs.next, name, maybeIcon, Some(description), maybeExportId, team.id, OffsetDateTime.now)
    val action = (all += raw).map(_ => tuple2Group((raw, team)))
    dataService.run(action)
  }

  def save(behaviorGroup: BehaviorGroup): Future[BehaviorGroup] = {
    val action = rawFindQuery(behaviorGroup.id).update(behaviorGroup.toRaw).map(_ => behaviorGroup)
    dataService.run(action)
  }

  def ensureExportIdFor(behaviorGroup: BehaviorGroup): Future[BehaviorGroup] = {
    if (behaviorGroup.maybeExportId.isDefined) {
      Future.successful(behaviorGroup)
    } else {
      val newExportId = Some(IDs.next)
      val action = uncompiledRawFindQuery(behaviorGroup.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => behaviorGroup.copy(maybeExportId = newExportId))
    }
  }

  def allFor(team: Team): Future[Seq[BehaviorGroup]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Group)
    }
    dataService.run(action)
  }

  def find(id: String): Future[Option[BehaviorGroup]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2Group)
    }
    dataService.run(action)
  }

  private def changeGroup(behavior: Behavior, newGroup: BehaviorGroup): DBIO[Behavior] = {
    BehaviorQueries.uncompiledFindRawQuery(behavior.id).map(_.groupId).update(Some(newGroup.id)).map { _ =>
      behavior.copy(maybeGroup = Some(newGroup))
    }
  }

  private def changeGroup(input: Input, newGroup: BehaviorGroup): DBIO[Input] = {
    InputQueries.uncompiledFindRawQuery(input.id).map(_.maybeBehaviorGroupId).update(Some(newGroup.id)).map { _ =>
      input.copy(maybeBehaviorGroup = Some(newGroup))
    }
  }

  private def moveChildren(fromGroup: BehaviorGroup, toGroup: BehaviorGroup): DBIO[BehaviorGroup] = {
    for {
      behaviorsToMove <- DBIO.from(dataService.behaviors.allForGroup(fromGroup))
      inputsToMove <- DBIO.from(dataService.inputs.allForGroup(fromGroup))
      _ <- DBIO.sequence(behaviorsToMove.map { ea =>
        changeGroup(ea, toGroup)
      })
      _ <- DBIO.sequence(inputsToMove.map { ea =>
        changeGroup(ea, toGroup)
      })
    } yield toGroup
  }

  def merge(groups: Seq[BehaviorGroup]): Future[BehaviorGroup] = {
    val firstGroup = groups.head
    val team = firstGroup.team
    val mergedName = groups.map(_.name).filter(_.trim.nonEmpty).mkString("-")
    val descriptions = groups.flatMap(_.maybeDescription).filter(_.trim.nonEmpty)
    val mergedDescription = if (descriptions.isEmpty) { None } else { Some(descriptions.mkString("\n")) }
    val maybeExportId = None // Don't think it makes sense to have an exportId for something merged
    val maybeIcon = groups.find(_.maybeIcon.isDefined).flatMap(_.maybeIcon)
    val rawMerged = RawBehaviorGroup(IDs.next, mergedName, maybeIcon, mergedDescription, maybeExportId, team.id, OffsetDateTime.now)
    val action = (for {
      merged <- (all += rawMerged).map(_ => tuple2Group((rawMerged, team)))
      _ <- DBIO.sequence(groups.map { ea =>
        moveChildren(ea, merged)
      })
      _ <- DBIO.sequence(groups.map { group =>
        all.filter(_.id === group.id).delete
      })
    } yield merged).transactionally

    dataService.run(action)
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

}
