package models.behaviors.savedanswer

import drivers.SlickPostgresDriver.api._

object SavedAnswerQueries {

  val all = TableQuery[SavedAnswersTable]

  def uncompiledFindQueryFor(inputId: Rep[String], maybeUserId: Rep[Option[String]]) = {
    all.
      filter { saved => saved.inputId === inputId }.
      filter { saved => (saved.maybeUserId.isEmpty && maybeUserId.isEmpty) || saved.maybeUserId === maybeUserId }
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
    all.
      filter { saved => (saved.maybeUserId.isEmpty && maybeUserId.isEmpty) || saved.maybeUserId === maybeUserId }.
      filter { saved => saved.inputId === inputId }
  }
  val maybeForQuery = Compiled(uncompiledMaybeForQuery _)

  def uncompiledAllForInputQuery(inputId: Rep[String]) = {
    all.filter { saved => saved.inputId === inputId }
  }
  val allForInputQuery = Compiled(uncompiledAllForInputQuery _)
}
