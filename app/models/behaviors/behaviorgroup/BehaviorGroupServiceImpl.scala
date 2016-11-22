package models.behaviors.behaviorgroup

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.input.{Input, InputQueries}
import models.team.Team
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class RawBehaviorGroup(id: String, name: String, teamId: String, createdAt: DateTime)

class BehaviorGroupsTable(tag: Tag) extends Table[RawBehaviorGroup](tag, "behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def teamId = column[String]("team_id")
  def createdAt = column[DateTime]("created_at")

  def * = (id, name, teamId, createdAt) <> ((RawBehaviorGroup.apply _).tupled, RawBehaviorGroup.unapply _)
}

class BehaviorGroupServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService]
                                        ) extends BehaviorGroupService {

  def dataService = dataServiceProvider.get

  import BehaviorGroupQueries._

  def createFor(name: String, team: Team): Future[BehaviorGroup] = {
    val raw = RawBehaviorGroup(IDs.next, name, team.id, DateTime.now)
    val action = (all += raw).map(_ => tuple2Group((raw, team)))
    dataService.run(action)
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
    val rawMerged = RawBehaviorGroup(IDs.next, mergedName, team.id, DateTime.now)
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

}
