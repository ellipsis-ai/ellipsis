package models.accounts.oauth2api

import slick.driver.PostgresDriver.api._

object OAuth2ApiQueries {

  val all = TableQuery[OAuth2ApisTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForQuery(maybeTeamId: Rep[Option[String]]) = all.filter(ea => ea.maybeTeamId.isEmpty || ea.maybeTeamId === maybeTeamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledFindGlobalByNameQuery(name: Rep[String]) = {
    all.filter(_.name === name).filter(_.maybeTeamId.isEmpty)
  }
  val findGlobalByNameQuery = Compiled(uncompiledFindGlobalByNameQuery _)

}
