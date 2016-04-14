package models

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class Team(
                 id: String,
                 name: String
                 ) {

  def save: DBIO[Team] = Team.save(this)

}

class TeamsTable(tag: Tag) extends Table[Team](tag, "teams") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")

  def * =
    (id, name) <> ((Team.apply _).tupled, Team.unapply _)
}

object Team {

  val all = TableQuery[TeamsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[Team]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def create: DBIO[Team] = Team(IDs.next, "").save

  def save(team: Team): DBIO[Team] = {
    val query = findQueryFor(team.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === team.id).update(team)
      }.getOrElse {
        all += team
      }.map { _ => team }
    }
  }
}
