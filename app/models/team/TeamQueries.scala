package models.team

import java.time.ZoneId

import drivers.SlickPostgresDriver.api._

class TeamsTable(tag: Tag) extends Table[Team](tag, "teams") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeTimeZone = column[Option[ZoneId]]("time_zone")

  def * =
    (id, name, maybeTimeZone) <> ((Team.apply _).tupled, Team.unapply _)
}

object TeamQueries {

  val all = TableQuery[TeamsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledFindByNameQueryFor(name: Rep[String]) = {
    all.filter(_.name === name)
  }
  val findByNameQueryFor = Compiled(uncompiledFindByNameQueryFor _)

}
