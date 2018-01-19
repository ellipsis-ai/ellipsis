package models.team

import java.time.{OffsetDateTime, ZoneId}
import drivers.SlickPostgresDriver.api._

class TeamsTable(tag: Tag) extends Table[Team](tag, "teams") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeTimeZone = column[Option[ZoneId]]("time_zone")
  def maybeOrganizationId = column[Option[String]]("organization_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, name, maybeTimeZone, maybeOrganizationId, createdAt) <> ((Team.apply _).tupled, Team.unapply _)
}

object TeamQueries {

  val all = TableQuery[TeamsTable]

  def uncompiledAllPagedQuery(offset: ConstColumn[Long], size: ConstColumn[Long]) = {
    all.drop(offset).take(size)
  }
  val allPagedQuery = Compiled(uncompiledAllPagedQuery _)


  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledFindByNameQueryFor(name: Rep[String]) = {
    all.filter(_.name === name)
  }
  val findByNameQueryFor = Compiled(uncompiledFindByNameQueryFor _)

}
