package models.behaviors.library

import drivers.SlickPostgresDriver.api._

object LibraryVersionQueries {

  val all = TableQuery[LibraryVersionsTable]

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    all.filter(_.behaviorGroupVersionId === groupVersionId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledFindByLibraryIdQuery(libraryId: Rep[String]) = {
    all.filter(_.libraryId === libraryId)
  }
  val findByLibraryIdQuery = Compiled(uncompiledFindByLibraryIdQuery _)

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
