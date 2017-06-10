package models.behaviors.defaultstorageitem

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroupQueries

object DefaultStorageItemQueries {

  val all = TableQuery[DefaultStorageItemsTable]
  val allWithBehaviorGroup = all.join(BehaviorGroupQueries.allWithTeam).on(_.behaviorGroupId === _._1.id)

  type TupleType = (RawDefaultStorageItem, BehaviorGroupQueries.TupleType)

  def tuple2Item(tuple: TupleType): DefaultStorageItem = {
    val raw = tuple._1
    val behaviorGroup = BehaviorGroupQueries.tuple2Group(tuple._2)
    DefaultStorageItem(raw.id, behaviorGroup, raw.data)
  }

  def uncompiledFindByIdQuery(id: Rep[String], behaviorGroupId: Rep[String]) = {
    allWithBehaviorGroup.filter { case(item, (group, _)) => item.id === id && group.id === behaviorGroupId }
  }
  val findByIdQuery = Compiled(uncompiledFindByIdQuery _)

}
