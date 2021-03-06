package models.accounts.oauth2api

import javax.inject.{Inject, Provider}

import models.IDs
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class OAuth2ApisTable(tag: Tag) extends Table[OAuth2Api](tag, "oauth2_apis") {

  implicit val grantTypeColumnType = MappedColumnType.base[OAuth2GrantType, String](
    { gt => gt.toString },
    { str => OAuth2GrantType.definitelyFind(str) }
  )

  def id = column[String]("id")
  def name = column[String]("name")
  def grantType = column[OAuth2GrantType]("grant_type")
  def maybeAuthorizationUrl = column[Option[String]]("authorization_url")
  def accessTokenUrl = column[String]("access_token_url")
  def maybeNewApplicationUrl = column[Option[String]]("new_application_url")
  def maybeScopeDocumentationUrl = column[Option[String]]("scope_documentation_url")
  def maybeTeamId = column[Option[String]]("team_id")
  def maybeAudience = column[Option[String]]("audience")

  def * = (id, name, grantType, maybeAuthorizationUrl, accessTokenUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, maybeTeamId, maybeAudience) <>
    ((OAuth2Api.apply _).tupled, OAuth2Api.unapply _)

}

class OAuth2ApiServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService],
                                       implicit val ec: ExecutionContext
                                     ) extends OAuth2ApiService {


  def dataService = dataServiceProvider.get

  import OAuth2ApiQueries._

  def findAction(id: String): DBIO[Option[OAuth2Api]] = {
    findQuery(id).result.map(_.headOption)
  }

  def find(id: String): Future[Option[OAuth2Api]] = {
    dataService.run(findAction(id))
  }

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth2Api]] = {
    dataService.run(allForQuery(maybeTeam.map(_.id)).result)
  }

  def save(api: OAuth2Api): Future[OAuth2Api] = {
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
