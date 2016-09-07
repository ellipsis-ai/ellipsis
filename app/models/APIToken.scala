package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.accounts.user.User
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class APIToken(
                    id: String,
                    label: String,
                    userId: String,
                    isRevoked: Boolean,
                    maybeLastUsed: Option[DateTime],
                    createdAt: DateTime
                    ) {
  val isValid: Boolean = !isRevoked

  val maybeLastUsedString: Option[String] = maybeLastUsed.map(_.toString(APITokenQueries.formatter))
}

class APITokensTable(tag: Tag) extends Table[APIToken](tag, "api_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def label = column[String]("label")
  def userId = column[String]("user_id")
  def isRevoked = column[Boolean]("is_revoked")
  def maybeLastUsed = column[Option[DateTime]]("last_used")
  def createdAt = column[DateTime]("created_at")

  def * = (id, label, userId, isRevoked, maybeLastUsed, createdAt) <> ((APIToken.apply _).tupled, APIToken.unapply _)
}

object APITokenQueries {

  val all = TableQuery[APITokensTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[APIToken]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def createFor(user: User, label: String): DBIO[APIToken] = {
    val newInstance = APIToken(IDs.next, label, user.id, isRevoked = false, None, DateTime.now)
    (all += newInstance).map(_ => newInstance)
  }

  def uncompiledAllForQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId).filterNot(_.isRevoked)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(user: User): DBIO[Seq[APIToken]] = {
    allForQuery(user.id).result
  }

  def use(token: APIToken): DBIO[APIToken] = {
    val updated = token.copy(maybeLastUsed = Some(DateTime.now))
    findQueryFor(token.id).update(updated).map(_ => updated)
  }

  def revoke(token: APIToken): DBIO[APIToken] = {
    val updated = token.copy(isRevoked = true)
    findQueryFor(token.id).update(updated).map(_ => updated)
  }

  val formatter = DateTimeFormat.forPattern("MMMM d, yyyy")

}
