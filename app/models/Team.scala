package models

import models.accounts.user.User
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class Team(
                 id: String,
                 name: String
                 ) {

  def save: DBIO[Team] = Team.save(this)

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

  def setInitialName(initialName: String): DBIO[Team] = {
    if (maybeNonEmptyName.isEmpty) {
      this.copy(name = initialName).save
    } else {
      DBIO.successful(this)
    }
  }

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
    for {
      maybeTeam <- find(id)
      canAccess <- maybeTeam.map { team =>
        user.canAccess(team)
      }.getOrElse(DBIO.successful(false))
    } yield if (canAccess) {
        maybeTeam
      } else {
        None
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

  def create(name: String): DBIO[Team] = Team(IDs.next, name).save

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
