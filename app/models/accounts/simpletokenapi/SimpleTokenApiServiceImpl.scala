package models.accounts.simpletokenapi

import javax.inject.{Inject, Provider}

import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleTokenApisTable(tag: Tag) extends Table[SimpleTokenApi](tag, "simple_token_apis") {

  def id = column[String]("id")
  def name = column[String]("name")
  def maybeTokenUrl = column[Option[String]]("token_url")
  def maybeTeamId = column[Option[String]]("team_id")

  def * = (id, name, maybeTokenUrl, maybeTeamId) <>
    ((SimpleTokenApi.apply _).tupled, SimpleTokenApi.unapply _)

}

class SimpleTokenApiServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService]
                                     ) extends SimpleTokenApiService {

  import SimpleTokenApiQueries._

  def dataService = dataServiceProvider.get

  def find(id: String): Future[Option[SimpleTokenApi]] = {
    dataService.run(findQuery(id).result.map(_.headOption))
  }

  def allFor(maybeTeam: Option[Team]): Future[Seq[SimpleTokenApi]] = {
    dataService.run(allForQuery(maybeTeam.map(_.id)).result)
  }

  def save(api: SimpleTokenApi): Future[SimpleTokenApi] = {
    val query = findQuery(api.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(api)
      }.getOrElse {
        all += api
      }.map(_ => api)
    }
    dataService.run(action)
  }


}
