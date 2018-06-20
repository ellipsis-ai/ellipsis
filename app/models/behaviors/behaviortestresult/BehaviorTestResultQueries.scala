package models.behaviors.behaviortestresult

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries

object BehaviorTestResultQueries {

  val all = TableQuery[BehaviorTestResultsTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1.id)

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(_, (_, ((bgv, _), _))) => bgv.id === groupVersionId }.
      map { case(r, _) => r }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
