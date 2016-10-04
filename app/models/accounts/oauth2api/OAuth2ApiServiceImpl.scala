package models.accounts.oauth2api

import javax.inject.{Inject, Provider}

import models.IDs
import models.team.Team
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def * = (id, name, grantType, maybeAuthorizationUrl, accessTokenUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, maybeTeamId) <>
    ((OAuth2Api.apply _).tupled, OAuth2Api.unapply _)

}

class OAuth2ApiServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService]
                                     ) extends OAuth2ApiService {


  def dataService = dataServiceProvider.get

  import OAuth2ApiQueries._

  def find(id: String): Future[Option[OAuth2Api]] = {
    dataService.run(findQuery(id).result.map(_.headOption))
  }

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth2Api]] = {
    dataService.run(allForQuery(maybeTeam.map(_.id)).result)
  }

  def createFor(
                 name: String,
                 grantType: OAuth2GrantType,
                 maybeAuthorizationUrl: Option[String],
                 accessTokenUrl: String,
                 maybeNewApplicationUrl: Option[String],
                 maybeScopeDocumentationUrl: Option[String]
               ): Future[OAuth2Api] = {
    val api = OAuth2Api(IDs.next, name, grantType, maybeAuthorizationUrl, accessTokenUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, None)
    dataService.run((all += api).map(_ => api))
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
