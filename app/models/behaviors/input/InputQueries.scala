package models.behaviors.input

import models.behaviors.behavior.BehaviorQueries
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType, TextType}
import slick.driver.PostgresDriver.api._

object InputQueries {

  val all = TableQuery[InputsTable]
  val joined = all.joinLeft(BehaviorQueries.allWithTeam).on(_.paramType === _._1.id)

  type TupleType = (RawInput, Option[BehaviorQueries.TupleType])

  def tuple2Input(tuple: TupleType): Input = {
    val raw = tuple._1
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.paramType).
        orElse(tuple._2.map { dataTypeBehavior => BehaviorBackedDataType(BehaviorQueries.tuple2Behavior(dataTypeBehavior)) }).
        getOrElse(TextType)
    Input(raw.id, raw.name, raw.maybeQuestion, paramType, raw.isSavedForTeam, raw.isSavedForUser)
  }

}
