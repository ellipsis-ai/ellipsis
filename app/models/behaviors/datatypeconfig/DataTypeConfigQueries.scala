package models.behaviors.datatypeconfig

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries

object DataTypeConfigQueries {

  val all = TableQuery[DataTypeConfigsTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1.id)

  type TupleType = (RawDataTypeConfig, BehaviorVersionQueries.TupleType)

  def tuple2Config(tuple: TupleType): DataTypeConfig = {
    val raw = tuple._1
    DataTypeConfig(raw.id, raw.maybeUsesCode, BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2))
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(_, bv) => bv._1._1.groupVersionId === groupVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledAllUsingDefaultStorageForQuery(groupVersionId: Rep[String]) = {
    uncompiledAllForQuery(groupVersionId).filter { case(config, _) => config.maybeUsesCode === false }
  }
  val allUsingDefaultStorageForQuery = Compiled(uncompiledAllUsingDefaultStorageForQuery _)

  def uncompiledMaybeForQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(_, bv) => bv._1._1.id === behaviorVersionId }
  }
  val maybeForQuery = Compiled(uncompiledMaybeForQuery _)

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithBehaviorVersion.filter { case(config, _) => config.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
