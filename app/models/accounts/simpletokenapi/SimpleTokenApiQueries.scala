package models.accounts.simpletokenapi

import slick.driver.PostgresDriver.api._

object SimpleTokenApiQueries {

  val all = TableQuery[SimpleTokenApisTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForQuery(maybeTeamId: Rep[Option[String]]) = all.filter(ea => ea.maybeTeamId.isEmpty || ea.maybeTeamId === maybeTeamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
