package models.behaviors.defaultstorageitem

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroupQueries
import play.api.libs.json.JsValue

object DefaultStorageItemQueries {

  val all = TableQuery[DefaultStorageItemsTable]
  val allWithBehaviorGroup = all.join(BehaviorGroupQueries.allWithTeam).on(_.behaviorGroupId === _._1.id)

  type TupleType = (RawDefaultStorageItem, BehaviorGroupQueries.TupleType)

  def tuple2Item(tuple: TupleType): DefaultStorageItem = {
    val raw = tuple._1
    val behaviorGroup = BehaviorGroupQueries.tuple2Group(tuple._2)
    DefaultStorageItem(raw.id, raw.typeName, behaviorGroup, raw.data)
  }

  def uncompiledFindByIdQuery(id: Rep[String], behaviorGroupId: Rep[String]) = {
    allWithBehaviorGroup.filter { case(item, (group, _)) => item.id === id && group.id === behaviorGroupId }
  }
  val findByIdQuery = Compiled(uncompiledFindByIdQuery _)

  def uncompiledByGroupAndTypeQuery(groupId: Rep[String], typeName: Rep[String]) = {
    allWithBehaviorGroup.
      filter { case(item, _) => item.behaviorGroupId === groupId }.
      filter { case(item, _) => item.typeName === typeName }
  }

  def uncompiledFilterQuery(groupId: Rep[String], typeName: Rep[String], filter: Rep[JsValue]) = {
    uncompiledByGroupAndTypeQuery(groupId, typeName).filter { case(item, _) => item.data.@>(filter) }
  }
  val filterQuery = Compiled(uncompiledFilterQuery _)

}
