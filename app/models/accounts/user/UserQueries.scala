package models.accounts.user

import drivers.SlickPostgresDriver.api._

class UsersTable(tag: Tag) extends Table[User](tag, "users") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeEmail = column[Option[String]]("email")

  def * =
    (id, teamId, maybeEmail) <> ((User.apply _).tupled, User.unapply _)
}

object UserQueries {
  val all = TableQuery[UsersTable]

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllForQuery(teamId: Rep[String]) = all.filter(_.teamId === teamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)
}

