package models.behaviors.behaviorparameter

import models.behaviors.behavior.BehaviorQueries
import models.behaviors.behaviorversion.BehaviorVersionQueries
import slick.driver.PostgresDriver.api._

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)
  val joined = allWithBehaviorVersion.joinLeft(BehaviorQueries.allWithTeam).on(_._1.paramType === _._1.id)

  type TupleType = ((RawBehaviorParameter, BehaviorVersionQueries.TupleType), Option[BehaviorQueries.TupleType])

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1._1
    val version = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._2)
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.paramType).
        orElse(tuple._2.map { dataTypeBehavior => BehaviorBackedDataType(BehaviorQueries.tuple2Behavior(dataTypeBehavior)) }).
        getOrElse(TextType)
    BehaviorParameter(
      raw.id,
      raw.name,
      raw.rank,
      version,
      raw.maybeQuestion,
      paramType
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    joined.
      filter { case((param, ((behaviorVersion, _), _)), _) => behaviorVersion.id === behaviorVersionId}.
      sortBy { case((param, ((behaviorVersion, _), _)), _) => param.rank.asc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
