package models.behaviors.datatypefield

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType, TextType}
import models.behaviors.datatypeconfig.DataTypeConfigQueries

object DataTypeFieldQueries {

  val all = TableQuery[DataTypeFieldsTable]
  val allWithDataTypeConfig = all.joinLeft(DataTypeConfigQueries.allWithBehaviorVersion).on(_.fieldTypeId === _._1.behaviorVersionId)

  type TupleType = (RawDataTypeField, Option[DataTypeConfigQueries.TupleType])

  def tuple2Field(tuple: TupleType): DataTypeField = {
    val raw = tuple._1
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.fieldTypeId).
        orElse(tuple._2.map { config => BehaviorBackedDataType(DataTypeConfigQueries.tuple2Config(config)) }).
        getOrElse(TextType)
    DataTypeField(raw.id, raw.fieldId, raw.name, paramType, raw.configId, raw.rank)
  }

  def uncompiledAllForConfigQuery(configId: Rep[String]) = {
    allWithDataTypeConfig.filter { case(field, _) => field.configId === configId }.sortBy { case(field, _) => field.rank.asc }
  }
  val allForConfigQuery = Compiled(uncompiledAllForConfigQuery _)

}
