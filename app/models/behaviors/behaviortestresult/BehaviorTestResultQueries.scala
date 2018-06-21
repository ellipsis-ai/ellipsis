package models.behaviors.behaviortestresult

import drivers.SlickPostgresDriver.api._

object BehaviorTestResultQueries {

  val all = TableQuery[BehaviorTestResultsTable]

  def uncompiledFindByBehaviorVersionQuery(behaviorVersionId: Rep[String]) = {
    all.filter(_.behaviorVersionId === behaviorVersionId)
  }
  val findByBehaviorVersionQuery = Compiled(uncompiledFindByBehaviorVersionQuery _)

}
