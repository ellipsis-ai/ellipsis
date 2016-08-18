package models.accounts

import models.{Team, IDs}
import org.apache.commons.lang.WordUtils
import play.api.libs.ws.{WSRequest, WSClient}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class OAuth2Application(
                              id: String,
                              name: String,
                              api: OAuth2Api,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String
                                ) {

  val authorizationUrl = api.authorizationUrl
  val accessTokenUrl = api.accessTokenUrl
  val scopeString = maybeScope.getOrElse("")

  def authorizationRequestFor(state: String, redirectUrl: String, ws: WSClient): WSRequest = {
    ws.url(authorizationUrl).withQueryString(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUrl,
      "scope" -> scopeString,
      "state" -> state,
      "access_type" -> "offline",
      "response_type" -> "code"
    )
  }

  def accessTokenRequestFor(code: String, redirectUrl: String, ws: WSClient): WSRequest = {
    ws.url(accessTokenUrl).withQueryString(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUrl)
  }

  def refreshTokenRequestFor(refreshToken: String, ws: WSClient): WSRequest = {
    ws.url(accessTokenUrl).withQueryString(
      "refresh_token" -> refreshToken,
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "grant_type" -> "refresh_token"
    )
  }

  def keyName: String = {
    val capitalized = WordUtils.capitalize(name).replaceAll("\\s", "")
    capitalized.substring(0, 1).toLowerCase() + capitalized.substring(1)
  }

  def toRaw = RawOAuth2Application(
    id,
    name,
    api.id,
    clientId,
    clientSecret,
    maybeScope,
    teamId
  )
}

case class RawOAuth2Application(
                                 id: String,
                                 name: String,
                                 apiId: String,
                                 clientId: String,
                                 clientSecret: String,
                                 maybeScope: Option[String],
                                 teamId: String
                                 )

class OAuth2ApplicationsTable(tag: Tag) extends Table[RawOAuth2Application](tag, "oauth2_applications") {
  def id = column[String]("id")
  def name = column[String]("name")
  def apiId = column[String]("api_id")
  def clientId = column[String]("client_id")
  def clientSecret = column[String]("client_secret")
  def maybeScope = column[Option[String]]("scope")
  def teamId = column[String]("team_id")

  def * = (id, name, apiId, clientId, clientSecret, maybeScope, teamId) <>
    ((RawOAuth2Application.apply _).tupled, RawOAuth2Application.unapply _)

}


object OAuth2ApplicationQueries {

  val all = TableQuery[OAuth2ApplicationsTable]
  val allWithApi = all.join(OAuth2ApiQueries.all).on(_.apiId === _.id)

  type TupleType = (RawOAuth2Application, OAuth2Api)

  def tuple2Application(tuple: TupleType): OAuth2Application = {
    val raw = tuple._1
    OAuth2Application(raw.id, raw.name, tuple._2, raw.clientId, raw.clientSecret, raw.maybeScope, raw.teamId)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithApi.
      filter { case(config, api) => config.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): DBIO[Option[OAuth2Application]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Application))
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithApi.filter { case(app, api) => app.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[OAuth2Application]] = {
    allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Application)
    }
  }

  def createFor(
                 api: OAuth2Api,
                 name: String,
                 clientId: String,
                 clientSecret: String,
                 maybeScope: Option[String],
                 teamId: String
                 ): DBIO[OAuth2Application] = {
    val raw = RawOAuth2Application(IDs.next, name, api.id, clientId, clientSecret, maybeScope, teamId)
    (all += raw).map { _ =>
      tuple2Application((raw, api))
    }
  }

  def update(config: OAuth2Application): DBIO[OAuth2Application] = {
    all.filter(_.id === config.id).update(config.toRaw).map( _ => config )
  }

}
