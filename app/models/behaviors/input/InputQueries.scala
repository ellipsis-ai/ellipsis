package models.behaviors.input

import models.behaviors.behavior.BehaviorQueries
import models.behaviors.behaviorgroup.BehaviorGroupQueries
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType, TextType}
import slick.driver.PostgresDriver.api._

object InputQueries {

  val all = TableQuery[InputsTable]
  val allWithGroup = all.joinLeft(BehaviorGroupQueries.allWithTeam).on(_.maybeBehaviorGroupId === _._1.id)
  val joined = allWithGroup.joinLeft(BehaviorQueries.allWithGroup).on(_._1.paramType === _._1._1.id)

  type TupleType = ((RawInput, Option[BehaviorGroupQueries.TupleType]), Option[BehaviorQueries.TupleType])

  def tuple2Input(tuple: TupleType): Input = {
    val raw = tuple._1._1
    val maybeGroup = tuple._1._2.map(BehaviorGroupQueries.tuple2Group)
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.paramType).
        orElse(tuple._2.map { dataTypeBehavior => BehaviorBackedDataType(BehaviorQueries.tuple2Behavior(dataTypeBehavior)) }).
        getOrElse(TextType)
    Input(raw.id, raw.name, raw.maybeQuestion, paramType, raw.isSavedForTeam, raw.isSavedForUser, maybeGroup)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    joined.filter { case((raw, _), _) => raw.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
