package models.storage.simplelist

import models.team.{Team, TeamQueries}
import drivers.SlickPostgresDriver.api._

object SimpleListQueries {

  val all = TableQuery[SimpleListsTable]
  val joined = all.join(TeamQueries.all).on(_.teamId === _.id)

  type TupleType = (RawSimpleList, Team)

  def tuple2List(tuple: TupleType): SimpleList = {
    val raw = tuple._1
    SimpleList(raw.id, tuple._2, raw.name, raw.createdAt)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    joined.filter { case(list, team) => list.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllForQuery(teamId: Rep[String]) = {
    joined.filter { case(list, team) => team.id === teamId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledRawFindByNameQueryFor(teamId: Rep[String], name: Rep[String]) = {
    all.filter(_.teamId === teamId).filter(_.name === name)
  }
  val rawFindByNameQueryFor = Compiled(uncompiledRawFindByNameQueryFor _)

}
