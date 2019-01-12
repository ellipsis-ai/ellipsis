package models.behaviors.library

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries

object LibraryVersionQueries {

  val all = TableQuery[LibraryVersionsTable]
  def allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.behaviorGroupVersionId === _._1._1.id)

  type TupleType = (RawLibraryVersion, BehaviorGroupVersionQueries.TupleType)
  type TableTupleType = (LibraryVersionsTable, BehaviorGroupVersionQueries.TableTupleType)

  def tuple2LibraryVersion(tuple: TupleType): LibraryVersion = {
    val raw = tuple._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._2)
    LibraryVersion(
      raw.id,
      raw.libraryId,
      raw.maybeExportId,
      raw.name,
      raw.maybeDescription,
      raw.functionBody,
      groupVersion,
      raw.createdAt
    )
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter(_._1.behaviorGroupVersionId === groupVersionId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledFindByLibraryIdQuery(libraryId: Rep[String], groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter(_._1.libraryId === libraryId).filter(_._1.behaviorGroupVersionId === groupVersionId)
  }
  val findByLibraryIdQuery = Compiled(uncompiledFindByLibraryIdQuery _)

  def uncompiledRawFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithGroupVersion.filter(_._1.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledFindCurrentForLibraryIdQuery(libraryId: Rep[String]) = {
    allWithGroupVersion.
      filter { case(lib, _) => lib.libraryId === libraryId }.
      sortBy { case(_, ((groupVersion, _), _)) => groupVersion.createdAt.desc }.
      take(1)
  }
  val findCurrentForLibraryIdQuery = Compiled(uncompiledFindCurrentForLibraryIdQuery _)

}
