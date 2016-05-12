package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class InvocationToken(
                 id: String,
                 teamId: String,
                 isUsed: Boolean,
                 createdAt: DateTime
                 ) {
  def isExpired: Boolean = createdAt.isBefore(DateTime.now.minusSeconds(30))
}

class InvocationTokensTable(tag: Tag) extends Table[InvocationToken](tag, "invocation_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def isUsed = column[Boolean]("is_used")
  def createdAt = column[DateTime]("created_at")

  def * = (id, teamId, isUsed, createdAt) <> ((InvocationToken.apply _).tupled, InvocationToken.unapply _)
}

object InvocationToken {

  val all = TableQuery[InvocationTokensTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[InvocationToken]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def createFor(team: Team): DBIO[InvocationToken] = {
    val newInstance = InvocationToken(IDs.next, team.id, isUsed = false, DateTime.now)
    (all += newInstance).map(_ => newInstance)
  }

  def use(token: InvocationToken): DBIO[InvocationToken] = {
    all.filter(_.id === token.id).map(_.isUsed).update(true).map { _ =>
      token.copy(isUsed = true)
    }
  }
}
