package models

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class Response(id: String, callId: String, text: String) {
  def save: DBIO[Response] = Response.save(this)
}

class ResponsesTable(tag: Tag) extends Table[Response](tag, "responses") {

  def id = column[String]("id", O.PrimaryKey)
  def callId = column[String]("call_id")
  def text = column[String]("text")

  def * = (id, callId, text) <> ((Response.apply _).tupled, Response.unapply _)
}

object Response {

  val all = TableQuery[ResponsesTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[Response]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def uncompiledFindByCallIdQuery(callId: Rep[String]) = {
    all.filter(_.callId === callId)
  }
  val findByCallIdQuery = Compiled(uncompiledFindByCallIdQuery _)

  def findByCallId(callId: String): DBIO[Option[Response]] = {
    findByCallIdQuery(callId).result.map(_.headOption)
  }

  def save(response: Response): DBIO[Response] = {
    val query = findQueryFor(response.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === response.id).update(response)
      }.getOrElse {
        all += response
      }.map { _ => response }
    }
  }

  def ensure(teamId: String, call: String, response: String): DBIO[Response] = {
    (for {
      call <- Call.ensure(call.trim, teamId)
      maybeExistingResponse <- findByCallId(call.id)
      response <- maybeExistingResponse.map { existing =>
        existing.copy(text = response).save
      }.getOrElse {
        Response(IDs.next, call.id, response.trim).save
      }
    } yield response).transactionally
  }
}
