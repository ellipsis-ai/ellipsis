package models.accounts

import com.mohiva.play.silhouette.impl.providers.OAuth2Settings
import models.Team
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class CustomOAuth2Configuration(
                              name: String,
                              authorizationUrl: String,
                              accessTokenUrl: String,
                              getProfileUrl: String,
                              getProfileJsonPath: String,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String
                                ) {

  def getProfilePathElements: Seq[String] = {
    getProfileJsonPath.split("\\.")
  }

  val redirectUrl: String = s"/link_oauth2/$name/$teamId"

  val oAuth2Settings: OAuth2Settings = OAuth2Settings(
    Some(authorizationUrl),
    accessTokenUrl,
    redirectUrl,
    clientId,
    clientSecret,
    maybeScope
  )
}

class CustomOAuth2ConfigurationsTable(tag: Tag) extends Table[CustomOAuth2Configuration](tag, "custom_oauth2_configurations") {
  def name = column[String]("name")
  def authorizationUrl = column[String]("authorization_url")
  def accessTokenUrl = column[String]("access_token_url")
  def getProfileUrl = column[String]("get_profile_url")
  def getProfileJsonPath = column[String]("get_profile_json_path")
  def clientId = column[String]("client_id")
  def clientSecret = column[String]("client_secret")
  def maybeScope = column[Option[String]]("scope")
  def teamId = column[String]("team_id")

  def * = (name, authorizationUrl, accessTokenUrl, getProfileUrl, getProfileJsonPath, clientId, clientSecret, maybeScope, teamId) <>
    ((CustomOAuth2Configuration.apply _).tupled, CustomOAuth2Configuration.unapply _)

}


object CustomOAuth2ConfigurationQueries {

  val all = TableQuery[CustomOAuth2ConfigurationsTable]

  def uncompiledFindQuery(name: Rep[String], teamId: Rep[String]) = {
    all.filter(_.name === name).filter(_.teamId === teamId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(name: String, team: Team): DBIO[Option[CustomOAuth2Configuration]] = {
    findQuery(name, team.id).result.map(_.headOption)
  }

  def ensureFor(
                 name: String,
                 authorizationUrl: String,
                 accessTokenUrl: String,
                 getProfileUrl: String,
                 getProfileJsonPath: String,
                 clientId: String,
                 clientSecret: String,
                 maybeScope: Option[String],
                 teamId: String
                 ): DBIO[CustomOAuth2Configuration] = {
    val query = findQuery(name, teamId)
    val config = CustomOAuth2Configuration(name, authorizationUrl, accessTokenUrl, getProfileUrl, getProfileJsonPath, clientId, clientSecret, maybeScope, teamId)
    query.result.headOption.flatMap {
      case Some(existing) => {
        query.update(config)
      }
      case None => all += config
    }.map(_ => config)
  }

}
