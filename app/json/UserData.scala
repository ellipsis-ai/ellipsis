package json

import models.team.Team

case class UserData(
                     id: String,
                     userName: Option[String],
                     fullName: Option[String],
                     tz: Option[String],
                     teamName: Option[String],
                     email: Option[String]
                   ) {
  val userNameOrDefault: String = userName.getOrElse(s"User with ID <$id>")
}

object UserData {
  def asAdmin(id: String): UserData = {
    UserData(id, Some("Ellipsis Admin"), Some("Ellipsis Admin"), None, Some("Ellipsis"), None)
  }

  def withoutProfile(id: String, maybeTeam: Option[Team]): UserData = {
    UserData(id, None, None, None, maybeTeam.map(_.name), None)
  }
}
