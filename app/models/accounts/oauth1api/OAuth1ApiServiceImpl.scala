package models.accounts.oauth1api

import javax.inject.{Inject, Provider}

import models.IDs
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class OAuth1ApisTable(tag: Tag) extends Table[OAuth1Api](tag, "oauth1_apis") {

  def id = column[String]("id")
  def name = column[String]("name")
  def requestTokenUrl = column[String]("request_token_url")
  def accessTokenUrl = column[String]("access_token_url")
  def authorizationUrl = column[String]("authorization_url")
  def maybeNewApplicationUrl = column[Option[String]]("new_application_url")
  def maybeScopeDocumentationUrl = column[Option[String]]("scope_documentation_url")
  def maybeTeamId = column[Option[String]]("team_id")

  def * = (id, name, requestTokenUrl, accessTokenUrl, authorizationUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, maybeTeamId) <>
    ((OAuth1Api.apply _).tupled, OAuth1Api.unapply _)

}

class OAuth1ApiServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService],
                                       implicit val ec: ExecutionContext
                                     ) extends OAuth1ApiService {


  def dataService = dataServiceProvider.get

  import OAuth1ApiQueries._

  def findAction(id: String): DBIO[Option[OAuth1Api]] = {
    findQuery(id).result.map(_.headOption)
  }

  def find(id: String): Future[Option[OAuth1Api]] = {
    dataService.run(findAction(id))
  }

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth1Api]] = {
    dataService.run(allForQuery(maybeTeam.map(_.id)).result)
  }

  def save(api: OAuth1Api): Future[OAuth1Api] = {
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
