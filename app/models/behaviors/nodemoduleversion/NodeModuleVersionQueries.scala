package models.behaviors.nodemoduleversion

import drivers.SlickPostgresDriver.api._

object NodeModuleVersionQueries {

  val all = TableQuery[NodeModuleVersionsTable]

  def uncompiledFindQuery(name: Rep[String], groupVersionId: Rep[String]) = {
    uncompiledAllForQuery(groupVersionId).filter(_.name === name)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    all.filter(_.groupVersionId === groupVersionId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
