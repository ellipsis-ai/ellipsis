package json

import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation

case class UserData(
                      ellipsisUserId: String,
                      context: Option[String],
                      userName: Option[String],
                      userIdForContext: Option[String],
                      fullName: Option[String],
                      email: Option[String],
                      timeZone: Option[String]
                    ) {
  val userNameOrDefault: String = userName.getOrElse(s"User with ID <$ellipsisUserId>")
}

object UserData {

  def fromSlackUserData(user: User, slackUserData: SlackUserData): UserData = {
    UserData(
      ellipsisUserId = user.id,
      context = Some(Conversation.SLACK_CONTEXT),
      userName = Some(slackUserData.getDisplayName),
      userIdForContext = Some(slackUserData.accountId),
      fullName = slackUserData.profile.flatMap(_.realName),
      email = slackUserData.profile.flatMap(_.email),
      timeZone = slackUserData.tz
    )
  }

  def asAdmin(id: String): UserData = {
    UserData(
      ellipsisUserId = id,
      context = None,
      userName = Some("Ellipsis Admin"),
      userIdForContext = None,
      fullName = Some("Ellipsis Admin"),
      email = None,
      timeZone = None
    )
  }

  def withoutProfile(id: String): UserData = {
    UserData(
      ellipsisUserId = id,
      context = None,
      userName = None,
      userIdForContext = None,
      fullName = None,
      email = None,
      timeZone = None
    )
  }
}
