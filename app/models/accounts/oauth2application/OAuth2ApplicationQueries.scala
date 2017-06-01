package models.accounts.oauth2application

import models.accounts.oauth2api.{OAuth2Api, OAuth2ApiQueries}
import drivers.SlickPostgresDriver.api._

object OAuth2ApplicationQueries {

  val all = TableQuery[OAuth2ApplicationsTable]
  val allWithApi = all.join(OAuth2ApiQueries.all).on(_.apiId === _.id)

  type TupleType = (RawOAuth2Application, OAuth2Api)

  def tuple2Application(tuple: TupleType): OAuth2Application = {
    val raw = tuple._1
    OAuth2Application(raw.id, raw.name, tuple._2, raw.clientId, raw.clientSecret, raw.maybeScope, raw.teamId, raw.isShared)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithApi.
      filter { case(app, api) => app.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithApi.filter { case(app, api) => app.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def uncompiledAllUsableForTeamQuery(teamId: Rep[String]) = {
    allWithApi.filter { case(app, api) => app.teamId === teamId || app.isShared }
  }
  val allUsableForTeamQuery = Compiled(uncompiledAllUsableForTeamQuery _)

}
