package models.behaviors.behaviorgroup

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
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

  def moveChildren(fromGroup: BehaviorGroup, toGroup: BehaviorGroup): Future[BehaviorGroup] = {
    for {
      behaviorsToMove <- dataService.behaviors.allForGroup(fromGroup)
      _ <- Future.sequence(behaviorsToMove.map { ea =>
        dataService.behaviors.changeGroup(ea, toGroup)
      })
      inputsToMove <- dataService.inputs.allFor(toGroup)
      _ <- Future.sequence(inputsToMove.map { ea =>
        dataService.inputs.changeGroup(ea, toGroup)
      })
    } yield toGroup
  }

  def merge(groups: Seq[BehaviorGroup]): Future[BehaviorGroup] = {
    val firstGroup = groups.head
    val team = firstGroup.team
    val mergedName = groups.map(_.name).mkString("-")
    val rawMerged = RawBehaviorGroup(IDs.next, mergedName, team.id, DateTime.now)
    val action = for {
      merged <- (all += rawMerged).map(_ => tuple2Group((rawMerged, team)))
      _ <- DBIO.sequence(groups.map { ea =>
        DBIO.from(moveChildren(ea, merged))
      })
    } yield merged
    dataService.run(action.transactionally)
  }

}
