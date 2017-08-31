package models.behaviors.nodemoduleversion

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NodeModuleVersionsTable(tag: Tag) extends Table[NodeModuleVersion](tag, "node_module_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def version = column[String]("version")
  def groupVersionId = column[String]("group_version_id")

  def * = (id, name, version, groupVersionId) <>
    ((NodeModuleVersion.apply _).tupled, NodeModuleVersion.unapply _)
}

class NodeModuleVersionServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService]
                                          ) extends NodeModuleVersionService {

  def dataService = dataServiceProvider.get

  import NodeModuleVersionQueries._

  def findAction(name: String, groupVersion: BehaviorGroupVersion): DBIO[Option[NodeModuleVersion]] = {
    findQuery(name, groupVersion.id).result.map { r =>
      r.headOption
    }
  }

  def ensureForAction(name: String, version: String, groupVersion: BehaviorGroupVersion): DBIO[NodeModuleVersion] = {
    findAction(name, groupVersion).flatMap { maybeVersion =>
      maybeVersion.map(DBIO.successful).getOrElse {
        val newInstance = NodeModuleVersion(IDs.next, name, version, groupVersion.id)
        (all += newInstance).map(_ => newInstance)
      }
    }
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[NodeModuleVersion]] = {
    val action = allForQuery(groupVersion.id).result
    dataService.run(action)
  }

}
