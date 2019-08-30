package models.behaviors.input

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType, TextType}
import models.behaviors.datatypeconfig.DataTypeConfigQueries

object InputQueries {

  val all = TableQuery[InputsTable]
  val joined = all.joinLeft(DataTypeConfigQueries.allWithBehaviorVersion).on(_.paramType === _._2._1._1.id)

  type TupleType = (RawInput, Option[DataTypeConfigQueries.TupleType])

  def tuple2Input(tuple: TupleType): Input = {
    val raw = tuple._1
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.paramType).
        orElse(tuple._2.map { config => BehaviorBackedDataType(DataTypeConfigQueries.tuple2Config(config)) }).
        getOrElse(TextType)
    Input(raw.id, raw.inputId, raw.maybeExportId, raw.name, raw.maybeQuestion, paramType, raw.isSavedForTeam, raw.isSavedForUser, raw.behaviorGroupVersionId)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    joined.filter { case(raw, _) => raw.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledFindByInputIdQuery(inputId: Rep[String], behaviorGroupVersionId: Rep[String]) = {
    joined.filter { case(raw, _) => raw.inputId === inputId && raw.behaviorGroupVersionId === behaviorGroupVersionId }
  }
  val findByInputIdQuery = Compiled(uncompiledFindByInputIdQuery _)

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)

  def uncompiledAllForGroupVersionQuery(groupVersionId: Rep[String]) = {
    joined.filter { case(raw, _) => raw.behaviorGroupVersionId === groupVersionId }
  }
  val allForGroupVersionQuery = Compiled(uncompiledAllForGroupVersionQuery _)

  def uncompiledFindByNameQuery(name: Rep[String], behaviorGroupVersionId: Rep[String]) = {
    joined.
      filter { case(raw, _) => raw.name === name }.
      filter { case(raw, _) => raw.behaviorGroupVersionId === behaviorGroupVersionId }
  }
  val findByNameQuery = Compiled(uncompiledFindByNameQuery _)

}
