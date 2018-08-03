package models.behaviors.events

import json.SlackUserData
import models.behaviors.conversations.conversation.Conversation

case class MessageUserData(
                            context: String,
                            userName: String,
                            ellipsisUserId: Option[String],
                            userIdForContext: Option[String],
                            fullName: Option[String],
                            email: Option[String],
                            timeZone: Option[String]
                          )

object MessageUserData {

  def fromSlackUserData(slackUserData: SlackUserData): MessageUserData = {
    MessageUserData(
      Conversation.SLACK_CONTEXT,
      userName = slackUserData.getDisplayName,
      ellipsisUserId = None,
      userIdForContext = Some(slackUserData.accountId),
      fullName = slackUserData.profile.flatMap(_.realName),
      email = slackUserData.profile.flatMap(_.email),
      timeZone = slackUserData.tz
    )
  }

}
