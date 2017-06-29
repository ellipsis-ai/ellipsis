package models.behaviors.defaultstorageitem

import drivers.SlickPostgresDriver.api._
import models.behaviors.behavior.BehaviorQueries
import play.api.libs.json.JsValue

object DefaultStorageItemQueries {

  val all = TableQuery[DefaultStorageItemsTable]
  val allWithBehavior = all.join(BehaviorQueries.allWithGroup).on(_.behaviorId === _._1._1.id)

  type TupleType = (RawDefaultStorageItem, BehaviorQueries.TupleType)

  def tuple2Item(tuple: TupleType): DefaultStorageItem = {
    val raw = tuple._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._2)
    DefaultStorageItem(raw.id, behavior, raw.data)
  }

  def uncompiledFindByIdQuery(id: Rep[String], behaviorGroupId: Rep[String]) = {
    allWithBehavior.filter { case(item, ((behavior, _), _)) => item.id === id && behavior.groupId === behaviorGroupId }
  }
  val findByIdQuery = Compiled(uncompiledFindByIdQuery _)

  def uncompiledByBehaviorIdQuery(behaviorId: Rep[String]) = {
    allWithBehavior.
      filter { case(item, ((behavior, _), _)) => behavior.id === behaviorId }
  }

  def uncompiledFilterQuery(behaviorId: Rep[String], filter: Rep[JsValue]) = {
    uncompiledByBehaviorIdQuery(behaviorId).filter { case(item, _) => item.data.@>(filter) }
  }
  val filterQuery = Compiled(uncompiledFilterQuery _)

}
