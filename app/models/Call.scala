package models

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class Call(id: String, text: String, teamId: String) {
  def save: DBIO[Call] = Call.save(this)
}

class CallsTable(tag: Tag) extends Table[Call](tag, "calls") {

  def id = column[String]("id", O.PrimaryKey)
  def text = column[String]("text")
  def teamId = column[String]("team_id")

  def * = (id, text, teamId) <> ((Call.apply _).tupled, Call.unapply _)
}

object Call {

  val all = TableQuery[CallsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[Call]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def save(call: Call): DBIO[Call] = {
    val query = findQueryFor(call.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === call.id).update(call)
      }.getOrElse {
        all += call
      }.map { _ => call }
    }
  }

  def create(text: String, teamId: String): DBIO[Call] = {
    Call(IDs.next, text, teamId).save
  }

  def uncompiledMatchForQuery(text: Rep[String], teamId: Rep[String]) = {
    all.
      filter(_.teamId === teamId).
      filter(_.text === text)
  }
  def matchForQuery = Compiled(uncompiledMatchForQuery _)

  def matchFor(text: String, teamId: String): DBIO[Option[Call]] = {
    matchForQuery(text, teamId).result.headOption
  }
}
