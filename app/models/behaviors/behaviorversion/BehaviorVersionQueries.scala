package models.behaviors.behaviorversion

import drivers.SlickPostgresDriver.api._
import models.behaviors.behavior.BehaviorQueries
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithBehavior = all.join(BehaviorQueries.allWithGroup).on(_.behaviorId === _._1._1.id)
  def allWithGroupVersion = allWithBehavior.join(BehaviorGroupVersionQueries.allWithUser).on(_._1.groupVersionId === _._1._1.id)

  type TupleType = ((RawBehaviorVersion, BehaviorQueries.TupleType), BehaviorGroupVersionQueries.TupleType)
  type TableTupleType = ((BehaviorVersionsTable, BehaviorQueries.TableTupleType), BehaviorGroupVersionQueries.TableTupleType)

  def uncompiledRawFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }

  def tuple2BehaviorVersion(tuple: TupleType): BehaviorVersion = {
    val raw = tuple._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._2)
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._1._2),
      groupVersion,
      raw.maybeDescription,
      raw.maybeName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      raw.responseType,
      raw.canBeMemoized,
      raw.isTest,
      raw.createdAt
    )
  }

  def uncompiledFindWithNameQuery(name: Rep[String], groupVersionId: Rep[String]) = {
    allWithGroupVersion.
      filter { case((version, _), _) => version.maybeName === name }.
      filter { case(_, ((groupVersion, _), _)) => groupVersion.id === groupVersionId }
  }
  val findWithNameQuery = Compiled(uncompiledFindWithNameQuery _)

}
