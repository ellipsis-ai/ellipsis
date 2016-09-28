package models.behaviors.behaviorparameter

import models.behaviors.behavior.{BehaviorQueries, RawBehavior}
import models.team.Team
import slick.driver.PostgresDriver.api._

object BehaviorBackedDataTypeQueries {

  val all = TableQuery[BehaviorBackedDataTypesTable]
  val joined = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  type TupleType = (RawBehaviorBackedDataType, (RawBehavior, Team))

  def tuple2DataType(tuple: TupleType): BehaviorBackedDataType = {
    val raw = tuple._1
    BehaviorBackedDataType(raw.id, raw.name, BehaviorQueries.tuple2Behavior(tuple._2))
  }

}
