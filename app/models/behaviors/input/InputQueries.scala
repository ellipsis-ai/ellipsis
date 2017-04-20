package models.behaviors.input

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType, TextType}
import models.behaviors.behaviorversion.BehaviorVersionQueries

object InputQueries {

  val all = TableQuery[InputsTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.behaviorGroupVersionId === _._1._1.id)
  val joined = allWithGroupVersion.joinLeft(BehaviorVersionQueries.allWithGroupVersion).on(_._1.paramType === _._1._1._1.id)

  type TupleType = ((RawInput, BehaviorGroupVersionQueries.TupleType), Option[BehaviorVersionQueries.TupleType])

  def tuple2Input(tuple: TupleType): Input = {
    val raw = tuple._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._1._2)
    val paramType =
      BehaviorParameterType.
        findBuiltIn(raw.paramType).
        orElse(tuple._2.map { dataTypeBehaviorVersion => BehaviorBackedDataType(BehaviorVersionQueries.tuple2BehaviorVersion(dataTypeBehaviorVersion)) }).
        getOrElse(TextType)
    Input(raw.id, raw.inputId, raw.maybeExportId, raw.name, raw.maybeQuestion, paramType, raw.isSavedForTeam, raw.isSavedForUser, groupVersion)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    joined.filter { case((raw, _), _) => raw.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledFindByInputIdQuery(inputId: Rep[String], behaviorGroupVersionId: Rep[String]) = {
    joined.filter { case((raw, _), _) => raw.inputId === inputId && raw.behaviorGroupVersionId === behaviorGroupVersionId }
  }
  val findByInputIdQuery = Compiled(uncompiledFindByInputIdQuery _)

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)

  def uncompiledAllForGroupVersionQuery(groupVersionId: Rep[String]) = {
    joined.filter { case((raw, _), _) => raw.behaviorGroupVersionId === groupVersionId }
  }
  val allForGroupVersionQuery = Compiled(uncompiledAllForGroupVersionQuery _)

}
