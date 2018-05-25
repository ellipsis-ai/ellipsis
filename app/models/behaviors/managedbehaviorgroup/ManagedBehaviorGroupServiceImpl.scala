package models.behaviors.managedbehaviorgroup

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class ManagedBehaviorGroupsTable(tag: Tag) extends Table[ManagedBehaviorGroup](tag, "managed_behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")
  def maybeContactId = column[Option[String]]("contact_id")

  def * =
    (id, groupId, maybeContactId) <>
      ((ManagedBehaviorGroup.apply _).tupled, ManagedBehaviorGroup.unapply _)
}

class ManagedBehaviorGroupServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService],
                                                     implicit val ec: ExecutionContext
                                                   ) extends ManagedBehaviorGroupService {

  def dataService = dataServiceProvider.get

  import ManagedBehaviorGroupQueries._

  def maybeForAction(group: BehaviorGroup): DBIO[Option[ManagedBehaviorGroup]] = {
    findForQuery(group.id).result.map(_.headOption)
  }

  def maybeFor(group: BehaviorGroup): Future[Option[ManagedBehaviorGroup]] = {
    dataService.run(maybeForAction(group))
  }

  def allFor(team: Team): Future[Seq[ManagedBehaviorGroup]] = {
    dataService.run(allForTeamQuery(team.id).result)
  }

  def ensureFor(group: BehaviorGroup, maybeContact: Option[User]): Future[ManagedBehaviorGroup] = {
    val action = findForQuery(group.id).result.flatMap { r =>
      r.headOption.map(DBIO.successful).getOrElse {
        val newInstance = ManagedBehaviorGroup(IDs.next, group.id, maybeContact.map(_.id))
        (all += newInstance).map(_ => newInstance)
      }
    }
    dataService.run(action)
  }

  def deleteFor(group: BehaviorGroup): Future[Unit] = {
    val action = findForQuery(group.id).delete.map(_ => {})
    dataService.run(action)
  }


}
