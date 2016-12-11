package models.storage.simplelist

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import org.joda.time.LocalDateTime
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.team.Team

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawSimpleList(
                          id: String,
                          teamId: String,
                          name: String,
                          createdAt: LocalDateTime
                        )

class SimpleListsTable(tag: Tag) extends Table[RawSimpleList](tag, "simple_lists") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def name = column[String]("name")
  def createdAt = column[LocalDateTime]("created_at")

  def * = (id, teamId, name, createdAt) <> ((RawSimpleList.apply _).tupled, RawSimpleList.unapply _)
}

class SimpleListServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService]
                                    ) extends SimpleListService {

  def dataService = dataServiceProvider.get

  import SimpleListQueries._

  def find(id: String): Future[Option[SimpleList]] = {
    dataService.run(findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2List)
    })
  }

  def createFor(team: Team, name: String): Future[SimpleList] = {
    val raw = RawSimpleList(IDs.next, team.id, name, LocalDateTime.now)
    dataService.run((all += raw).map { _ =>
      SimpleList(raw.id, team, raw.name, raw.createdAt)
    })
  }

  def ensureFor(team: Team, name: String): Future[SimpleList] = {
    val query = rawFindByNameQueryFor(team.id, name)
    val action = query.result.flatMap { r =>
      r.headOption.map(DBIO.successful).getOrElse {
        val raw = RawSimpleList(IDs.next, team.id, name, LocalDateTime.now)
        (all += raw).map(_ => raw)
      }
    }.map { raw =>
      SimpleList(raw.id, team, raw.name, raw.createdAt)
    }
    dataService.run(action)
  }

  def allFor(team: Team): Future[Seq[SimpleList]] = {
    dataService.run(allForQuery(team.id).result.map { r =>
      r.map(tuple2List)
    })
  }

}
