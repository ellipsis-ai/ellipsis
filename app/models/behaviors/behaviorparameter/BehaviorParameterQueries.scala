package models.behaviors.behaviorparameter

import models.behaviors.input.InputQueries
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithInput = all.
    join(InputQueries.joined).on(_.inputId === _._1.inputId).
    join(BehaviorVersionQueries.all).on(_._1.behaviorVersionId === _.id).
    filter { case((_, (input, _)), behaviorVersion) => input.behaviorGroupVersionId === behaviorVersion.groupVersionId }.
    map { case(withInput, _) => withInput }

  type TupleType = (RawBehaviorParameter, InputQueries.TupleType)

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1
    val input = InputQueries.tuple2Input(tuple._2)
    BehaviorParameter(
      raw.id,
      raw.rank,
      input,
      raw.behaviorVersionId
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithInput.
      filter { case(param, _) => param.behaviorVersionId === behaviorVersionId}.
      sortBy { case(param, _) => param.rank.asc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
