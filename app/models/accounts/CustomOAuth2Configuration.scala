package models.accounts

import models.{IDs, Team}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class CustomOAuth2Configuration(
                                      id: String,
                                      name: String,
                                      template: CustomOAuth2ConfigurationTemplate,
                                      clientId: String,
                                      clientSecret: String,
                                      maybeScope: Option[String],
                                      teamId: String
                                        ) {

  val authorizationUrl = template.authorizationUrl
  val accessTokenUrl = template.accessTokenUrl
  val scopeString = maybeScope.getOrElse("")

  def authorizationUrlFor(state: String, redirectUrl: String): String = {
    s"$authorizationUrl?client_id=$clientId&redirect_uri=$redirectUrl&scope=$scopeString&state=$state"
  }

  def toRaw = RawCustomOAuth2Configuration(
    id,
    name,
    template.id,
    clientId,
    clientSecret,
    maybeScope,
    teamId
  )
}

case class RawCustomOAuth2Configuration(
                                         id: String,
                                         name: String,
                                         templateId: String,
                                         clientId: String,
                                         clientSecret: String,
                                         maybeScope: Option[String],
                                         teamId: String
                                         )

class CustomOAuth2ConfigurationsTable(tag: Tag) extends Table[RawCustomOAuth2Configuration](tag, "custom_oauth2_configurations") {
  def id = column[String]("id")
  def name = column[String]("name")
  def templateId = column[String]("template_id")
  def clientId = column[String]("client_id")
  def clientSecret = column[String]("client_secret")
  def maybeScope = column[Option[String]]("scope")
  def teamId = column[String]("team_id")

  def * = (id, name, templateId, clientId, clientSecret, maybeScope, teamId) <>
    ((RawCustomOAuth2Configuration.apply _).tupled, RawCustomOAuth2Configuration.unapply _)

}


object CustomOAuth2ConfigurationQueries {

  val all = TableQuery[CustomOAuth2ConfigurationsTable]
  val allWithTemplate = all.join(CustomOAuth2ConfigurationTemplateQueries.all).on(_.templateId === _.id)

  type TupleType = (RawCustomOAuth2Configuration, CustomOAuth2ConfigurationTemplate)

  def tuple2Config(tuple: TupleType): CustomOAuth2Configuration = {
    val raw = tuple._1
    CustomOAuth2Configuration(raw.id, raw.name, tuple._2, raw.clientId, raw.clientSecret, raw.maybeScope, raw.teamId)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTemplate.
      filter { case(config, template) => config.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): DBIO[Option[CustomOAuth2Configuration]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Config))
  }

  def createFor(
                 template: CustomOAuth2ConfigurationTemplate,
                 name: String,
                 clientId: String,
                 clientSecret: String,
                 maybeScope: Option[String],
                 teamId: String
                 ): DBIO[CustomOAuth2Configuration] = {
    val raw = RawCustomOAuth2Configuration(IDs.next, name, template.id, clientId, clientSecret, maybeScope, teamId)
    (all += raw).map { _ =>
      tuple2Config((raw, template))
    }
  }

  def update(config: CustomOAuth2Configuration): DBIO[CustomOAuth2Configuration] = {
    all.filter(_.id === config.id).update(config.toRaw).map( _ => config )
  }

}
