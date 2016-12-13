package models.behaviors.savedanswer

import models.behaviors.input.InputQueries
import drivers.SlickPostgresDriver.api._

object SavedAnswerQueries {

  val all = TableQuery[SavedAnswersTable]
  val allWithInput = all.join(InputQueries.joined).on(_.inputId === _._1._1.id)

  type TupleType = (RawSavedAnswer, InputQueries.TupleType)

  def tuple2SavedAnswer(tuple: TupleType): SavedAnswer = {
    val raw = tuple._1
    val input = InputQueries.tuple2Input(tuple._2)
    SavedAnswer(raw.id, input, raw.valueString, raw.maybeUserId, raw.createdAt)
  }

  def uncompiledFindQueryFor(inputId: Rep[String], maybeUserId: Rep[Option[String]]) = {
    allWithInput.
      filter { case(saved, ((input, _), _)) => input.id === inputId }.
      filter { case(saved, _) => (saved.maybeUserId.isEmpty && maybeUserId.isEmpty) || saved.maybeUserId === maybeUserId }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledRawFindQueryFor(inputId: Rep[String], maybeUserId: Rep[Option[String]]) = {
    all.
      filter(_.inputId === inputId).
      filter { saved => (saved.maybeUserId.isEmpty && maybeUserId.isEmpty) || saved.maybeUserId === maybeUserId }
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQueryFor _)

  def uncompiledRawFindQueryIgnoringUserFor(inputId: Rep[String]) = {
    all.filter(_.inputId === inputId)
  }
  val rawFindQueryIgnoringUserFor = Compiled(uncompiledRawFindQueryIgnoringUserFor _)

  def uncompiledMaybeForQuery(maybeUserId: Rep[Option[String]], inputId: Rep[String]) = {
    allWithInput.
      filter { case(saved, _) => (saved.maybeUserId.isEmpty && maybeUserId.isEmpty) || saved.maybeUserId === maybeUserId }.
      filter { case(saved, ((input, _), _)) => input.id === inputId }
  }
  val maybeForQuery = Compiled(uncompiledMaybeForQuery _)

  def uncompiledAllForInputQuery(inputId: Rep[String]) = {
    allWithInput.filter { case(saved, _) => saved.inputId === inputId }
  }
  val allForInputQuery = Compiled(uncompiledAllForInputQuery _)
}
