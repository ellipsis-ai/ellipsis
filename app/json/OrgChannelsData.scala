package json

case class OrgChannelsData(
                             dmChannels: Seq[ScheduleChannelData],
                             mpimChannels: Seq[ScheduleChannelData],
                             orgSharedChannels: Seq[ScheduleChannelData],
                             externallySharedChannels: Seq[ScheduleChannelData],
                             teamChannels: Seq[TeamChannelsData]
                           )
