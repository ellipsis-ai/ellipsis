package models.behaviors.library

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries

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
    all.join(BehaviorGroupVersionQueries.all).on(_.behaviorGroupVersionId === _.id).
      filter { case(lib, _) => lib.libraryId === libraryId }.
      sortBy { case(_, groupVersion) => groupVersion.createdAt.desc }.
      map { case(lib, _) => lib }.
      take(1)
  }
  val findCurrentForLibraryIdQuery = Compiled(uncompiledFindCurrentForLibraryIdQuery _)

}
