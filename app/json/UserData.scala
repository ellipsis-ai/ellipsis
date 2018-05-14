package json

case class UserData(
                     id: String,
                     userName: Option[String],
                     fullName: Option[String],
                     tz: Option[String],
                     teamName: Option[String]
                   )

object UserData {
  def asAdmin(id: String): UserData = {
    UserData(id, Some("Ellipsis Admin"), Some("Ellipsis Admin"), None, Some("Ellipsis"))
  }
}
