package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersionQueries
import models.behaviors.input.InputQueries
import slick.driver.PostgresDriver.api._

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithInput = all.join(InputQueries.joined).on(_.inputId === _._1.id)
  val allWithBehaviorVersion = allWithInput.join(BehaviorVersionQueries.allWithBehavior).on(_._1.behaviorVersionId === _._1._1.id)

  type TupleType = (((RawBehaviorParameter, InputQueries.TupleType), BehaviorVersionQueries.TupleType))

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1._1
    val version = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2)
    val input = InputQueries.tuple2Input(tuple._1._2)
    BehaviorParameter(
      raw.id,
      raw.rank,
      input,
      version
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case((param, input), ((behaviorVersion, _), _)) => behaviorVersion.id === behaviorVersionId}.
      sortBy { case((param, input), ((behaviorVersion, _), _)) => param.rank.asc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
