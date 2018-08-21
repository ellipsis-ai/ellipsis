package models.accounts.oauth1application

import drivers.SlickPostgresDriver.api._
import models.accounts.oauth1api.{OAuth1Api, OAuth1ApiQueries}

object OAuth1ApplicationQueries {

  val all = TableQuery[OAuth1ApplicationsTable]
  val allWithApi = all.join(OAuth1ApiQueries.all).on(_.apiId === _.id)

  type TupleType = (RawOAuth1Application, OAuth1Api)

  def tuple2Application(tuple: TupleType): OAuth1Application = {
    val raw = tuple._1
    OAuth1Application(raw.id, raw.name, tuple._2, raw.consumerKey, raw.consumerSecret, raw.teamId, raw.isShared)
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
