package models.behaviors.events

import json.SlackUserData
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation

case class EventUserData(
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

object EventUserData {

  def fromSlackUserData(user: User, slackUserData: SlackUserData): EventUserData = {
    EventUserData(
      ellipsisUserId = user.id,
      context = Some(Conversation.SLACK_CONTEXT),
      userName = Some(slackUserData.getDisplayName),
      userIdForContext = Some(slackUserData.accountId),
      fullName = slackUserData.profile.flatMap(_.realName),
      email = slackUserData.profile.flatMap(_.email),
      timeZone = slackUserData.tz
    )
  }

  def asAdmin(id: String): EventUserData = {
    EventUserData(
      ellipsisUserId = id,
      context = None,
      userName = Some("Ellipsis Admin"),
      userIdForContext = None,
      fullName = Some("Ellipsis Admin"),
      email = None,
      timeZone = None
    )
  }

  def withoutProfile(id: String): EventUserData = {
    EventUserData(
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
