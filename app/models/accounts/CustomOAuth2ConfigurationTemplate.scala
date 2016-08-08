package models.accounts

import models.{Team, IDs}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class CustomOAuth2ConfigurationTemplate(
                                              id: String,
                                              name: String,
                                              authorizationUrl: String,
                                              accessTokenUrl: String,
                                              getProfileUrl: String,
                                              getProfileJsonPath: String,
                                              maybeTeamId: Option[String]
                                              ) {

}

class CustomOAuth2ConfigurationTemplatesTable(tag: Tag) extends Table[CustomOAuth2ConfigurationTemplate](tag, "custom_oauth2_configuration_templates") {
  def id = column[String]("id")
  def name = column[String]("name")
  def authorizationUrl = column[String]("authorization_url")
  def accessTokenUrl = column[String]("access_token_url")
  def getProfileUrl = column[String]("get_profile_url")
  def getProfileJsonPath = column[String]("get_profile_json_path")
  def maybeTeamId = column[Option[String]]("team_id")

  def * = (id, name, authorizationUrl, accessTokenUrl, getProfileUrl, getProfileJsonPath, maybeTeamId) <>
    ((CustomOAuth2ConfigurationTemplate.apply _).tupled, CustomOAuth2ConfigurationTemplate.unapply _)

}


object CustomOAuth2ConfigurationTemplateQueries {

  val all = TableQuery[CustomOAuth2ConfigurationTemplatesTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): DBIO[Option[CustomOAuth2ConfigurationTemplate]] = {
    findQuery(id).result.map(_.headOption)
  }

  def uncompiledAllForQuery(maybeTeamId: Rep[Option[String]]) = all.filter(ea => ea.maybeTeamId.isEmpty || ea.maybeTeamId === maybeTeamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(maybeTeam: Option[Team]): DBIO[Seq[CustomOAuth2ConfigurationTemplate]] = {
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
                 getProfileUrl: String,
                 getProfileJsonPath: String
                 ): DBIO[CustomOAuth2ConfigurationTemplate] = {
    val template = CustomOAuth2ConfigurationTemplate(IDs.next, name, authorizationUrl, accessTokenUrl, getProfileUrl, getProfileJsonPath, None)
    (all += template).map(_ => template)
  }

  def save(template: CustomOAuth2ConfigurationTemplate): DBIO[CustomOAuth2ConfigurationTemplate] = {
    val query = findQuery(template.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(template)
      }.getOrElse {
        all += template
      }.map(_ => template)
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
