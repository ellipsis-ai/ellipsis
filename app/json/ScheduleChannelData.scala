package json

import utils.SlackConversation

case class ScheduleChannelData(
                                id: String,
                                name: String,
                                context: String,
                                members: Seq[String],
                                isPublic: Boolean,
                                isArchived: Boolean
                              )

object ScheduleChannelData {
  def fromSlackConversationList(list: Seq[SlackConversation]): Seq[ScheduleChannelData] = {
    list.map((ea) => ScheduleChannelData(ea.id, ea.computedName, "Slack", ea.membersList, ea.isPublic, ea.isArchived))
  }
}
