package models

import models.accounts.User
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

  def find(id: String, user: User): DBIO[Option[Team]] = {
    find(id).map { maybeTeam =>
      maybeTeam.flatMap { team =>
        if (user.canAccess(team)) {
          Some(team)
        } else {
          None
        }
      }
    }
  }

  def findForToken(tokenId: String): DBIO[Option[Team]] = {
    for {
      maybeToken <- InvocationToken.find(tokenId)
      maybeTeam <- maybeToken.map { token =>
        if (token.isExpired || token.isUsed) {
          DBIO.successful(None)
        } else {
          InvocationToken.use(token).flatMap { _ =>
            find(token.teamId)
          }
        }
      }.getOrElse(DBIO.successful(None))
    } yield maybeTeam
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
