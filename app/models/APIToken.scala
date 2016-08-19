package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class APIToken(
                    id: String,
                    label: String,
                    teamId: String,
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
  def teamId = column[String]("team_id")
  def isRevoked = column[Boolean]("is_revoked")
  def maybeLastUsed = column[Option[DateTime]]("last_used")
  def createdAt = column[DateTime]("created_at")

  def * = (id, label, teamId, isRevoked, maybeLastUsed, createdAt) <> ((APIToken.apply _).tupled, APIToken.unapply _)
}

object APITokenQueries {

  val all = TableQuery[APITokensTable]

  def uncompiledFindQueryFor(id: Rep[String], teamId: Rep[String]) = {
    all.filter(_.id === id).filter(_.teamId === teamId)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String, team: Team): DBIO[Option[APIToken]] = {
    findQueryFor(id, team.id).result.map(_.headOption)
  }

  def createFor(team: Team, label: String): DBIO[APIToken] = {
    val newInstance = APIToken(IDs.next, label, team.id, isRevoked = false, None, DateTime.now)
    (all += newInstance).map(_ => newInstance)
  }

  def uncompiledFindValidForQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId).filterNot(_.isRevoked)
  }
  val findValidForQuery = Compiled(uncompiledFindValidForQuery _)

  def findValidFor(team: Team): DBIO[Option[APIToken]] = {
    findValidForQuery(team.id).result.map { r =>
      r.headOption
    }
  }

  def uncompiledAllForQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(team: Team): DBIO[Seq[APIToken]] = {
    allForQuery(team.id).result
  }

  def use(token: APIToken, team: Team): DBIO[APIToken] = {
    val updated = token.copy(maybeLastUsed = Some(DateTime.now))
    findQueryFor(token.id, team.id).update(updated).map(_ => updated)
  }

  val formatter = DateTimeFormat.forPattern("MMMM d, yyyy")

}
