package models.behaviors.config.awsconfig

import drivers.SlickPostgresDriver.api._

object AWSConfigQueries {

  val all = TableQuery[AWSConfigsTable]

  def uncompiledAllForQuery(teamId: Rep[String]) = all.filter((_.teamId === teamId))
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledFindQuery(id: Rep[String]) = all.filter(_.id === id)
  val findQuery = Compiled(uncompiledFindQuery _)

}
