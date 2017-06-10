package models.behaviors.datatypeconfig

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries

object DataTypeConfigQueries {

  val all = TableQuery[DataTypeConfigsTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1._1.id)

  type TupleType = (RawDataTypeConfig, BehaviorVersionQueries.TupleType)

  def tuple2Config(tuple: TupleType): DataTypeConfig = {
    DataTypeConfig(tuple._1.id, BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2))
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(config, bv) => bv._1._1._1.groupVersionId === groupVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
