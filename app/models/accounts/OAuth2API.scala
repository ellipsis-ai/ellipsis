package models.accounts

import models.{Team, IDs}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class OAuth2Api(
                      id: String,
                      name: String,
                      authorizationUrl: String,
                      accessTokenUrl: String,
                      maybeNewApplicationUrl: Option[String],
                      maybeScopeDocumentationUrl: Option[String],
                      maybeTeamId: Option[String]
                      ) {

}

class OAuth2ApisTable(tag: Tag) extends Table[OAuth2Api](tag, "oauth2_apis") {
  def id = column[String]("id")
  def name = column[String]("name")
  def authorizationUrl = column[String]("authorization_url")
  def accessTokenUrl = column[String]("access_token_url")
  def maybeNewApplicationUrl = column[Option[String]]("new_application_url")
  def maybeScopeDocumentationUrl = column[Option[String]]("scope_documentation_url")
  def maybeTeamId = column[Option[String]]("team_id")

  def * = (id, name, authorizationUrl, accessTokenUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, maybeTeamId) <>
    ((OAuth2Api.apply _).tupled, OAuth2Api.unapply _)

}


object OAuth2ApiQueries {

  val all = TableQuery[OAuth2ApisTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): DBIO[Option[OAuth2Api]] = {
    findQuery(id).result.map(_.headOption)
  }

  def uncompiledAllForQuery(maybeTeamId: Rep[Option[String]]) = all.filter(ea => ea.maybeTeamId.isEmpty || ea.maybeTeamId === maybeTeamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(maybeTeam: Option[Team]): DBIO[Seq[OAuth2Api]] = {
    allForQuery(maybeTeam.map(_.id)).result
  }

  def uncompiledFindGlobalByNameQuery(name: Rep[String]) = {
    all.filter(_.name === name).filter(_.maybeTeamId.isEmpty)
  }
  val findGlobalByNameQuery = Compiled(uncompiledFindGlobalByNameQuery _)

  def createFor(
                 name: String,
                 authorizationUrl: String,
                 accessTokenUrl: String,
                 maybeNewApplicationUrl: Option[String],
                 maybeScopeDocumentationUrl: Option[String]
                 ): DBIO[OAuth2Api] = {
    val api = OAuth2Api(IDs.next, name, authorizationUrl, accessTokenUrl, maybeNewApplicationUrl, maybeScopeDocumentationUrl, None)
    (all += api).map(_ => api)
  }

  def save(api: OAuth2Api): DBIO[OAuth2Api] = {
    val query = findQuery(api.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(api)
      }.getOrElse {
        all += api
      }.map(_ => api)
    }
  }

//  val github = ensureFor(
//    "Github",
//    "https://github.com/login/oauth/authorize",
//    "https://github.com/login/oauth/access_token",
//    "https://api.github.com/user?access_token=%s",
//    "id"
//  )
//
//  val todoist = ensureFor(
//    "Todoist",
//    "https://todoist.com/oauth/authorize",
//    "https://todoist.com/oauth/access_token",
//    """https://todoist.com/API/v7/sync?token=%s&resource_types=["user"]&sync_token="*"""",
//    "user.id"
//  )

}
