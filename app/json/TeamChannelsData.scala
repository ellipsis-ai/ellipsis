package json

case class TeamChannelsData(
                             teamName: String,
                             channelList: Seq[ScheduleChannelData]
                            ) {
  def copyWithoutCommonChannels: TeamChannelsData = {
    val channels =
      channelList.
        filterNot(_.isOrgShared).
        filterNot(_.isExternallyShared).
        filterNot(_.isOtherDm).
        filterNot(_.isSelfDm).
        filterNot(_.isPrivateGroup)
    copy(channelList = channels)
  }
}
