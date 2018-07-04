package json

import models.team.Team

case class UserData(
                     id: String,
                     userName: Option[String],
                     fullName: Option[String],
                     tz: Option[String],
                     teamName: Option[String],
                     email: Option[String],
                     slackUserData: Option[SlackUserData]
                   ) {
  val userNameOrDefault: String = userName.getOrElse(s"User with ID <$id>")
}

object UserData {
  def asAdmin(id: String): UserData = {
    UserData(
      id = id,
      userName = Some("Ellipsis Admin"),
      fullName = Some("Ellipsis Admin"),
      tz = None,
      teamName = Some("Ellipsis"),
      email = None,
      slackUserData = None
    )
  }

  def withoutProfile(id: String, maybeTeam: Option[Team]): UserData = {
    UserData(
      id = id,
      userName = None,
      fullName = None,
      tz = None,
      teamName = maybeTeam.map(_.name),
      email = None,
      slackUserData = None
    )
  }
}
