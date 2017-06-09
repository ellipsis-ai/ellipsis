package json

import utils.ChannelLike

case class ScheduleChannelData(
                                id: String,
                                name: String,
                                context: String,
                                members: Seq[String],
                                isPublic: Boolean
                              )

object ScheduleChannelData {
  def fromChannelLikeList(list: Seq[ChannelLike]): Seq[ScheduleChannelData] = {
    list.map((ea) => ScheduleChannelData(ea.id, ea.name, "Slack", ea.members, ea.isPublic))
  }
}
