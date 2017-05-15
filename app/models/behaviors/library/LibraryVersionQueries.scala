package models.behaviors.library

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroupQueries

object LibraryVersionQueries {

  val all = TableQuery[LibraryVersionsTable]

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    all.filter(_.behaviorGroupVersionId === groupVersionId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledFindByLibraryIdQuery(libraryId: Rep[String], groupVersionId: Rep[String]) = {
    all.filter(_.libraryId === libraryId).filter(_.behaviorGroupVersionId === groupVersionId)
  }
  val findByLibraryIdQuery = Compiled(uncompiledFindByLibraryIdQuery _)

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledFindCurrentForLibraryIdQuery(libraryId: Rep[String]) = {
    all.join(BehaviorGroupQueries.all).on(_.behaviorGroupVersionId === _.maybeCurrentVersionId).
      filter { case(lib, group) => lib.libraryId === libraryId }.
      map { case(lib, group) => lib }
  }
  val findCurrentForLibraryIdQuery = Compiled(uncompiledFindCurrentForLibraryIdQuery _)

}
