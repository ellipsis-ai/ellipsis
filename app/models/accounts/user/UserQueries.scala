package models.accounts.user

import slick.driver.PostgresDriver.api._

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
}

