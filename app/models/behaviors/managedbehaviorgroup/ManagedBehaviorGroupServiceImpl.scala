package models.behaviors.managedbehaviorgroup

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class ManagedBehaviorGroupsTable(tag: Tag) extends Table[ManagedBehaviorGroup](tag, "managed_behavior_groups") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")

  def * =
    (id, groupId) <>
      ((ManagedBehaviorGroup.apply _).tupled, ManagedBehaviorGroup.unapply _)
}

class ManagedBehaviorGroupServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService],
                                                     implicit val ec: ExecutionContext
                                                   ) extends ManagedBehaviorGroupService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[ManagedBehaviorGroupsTable]

  def uncompiledFindForQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId)
  }
  val findForQuery = Compiled(uncompiledFindForQuery _)

  def maybeForAction(group: BehaviorGroup): DBIO[Option[ManagedBehaviorGroup]] = {
    findForQuery(group.id).result.map(_.headOption)
  }

  def maybeFor(group: BehaviorGroup): Future[Option[ManagedBehaviorGroup]] = {
    dataService.run(maybeForAction(group))
  }

  def ensureForAction(group: BehaviorGroup): DBIO[ManagedBehaviorGroup] = {
    findForQuery(group.id).result.flatMap { r =>
      r.headOption.map(DBIO.successful).getOrElse {
        val newInstance = ManagedBehaviorGroup(IDs.next, group.id)
        (all += newInstance).map(_ => newInstance)
      }
    }
  }


}
