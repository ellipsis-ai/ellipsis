package json


case class ScheduleChannelData(
                                id: String,
                                name: String,
                                context: String,
                                isBotMember: Boolean,
                                isSelfDm: Boolean,
                                isOtherDm: Boolean,
                                isPrivateChannel: Boolean,
                                isPrivateGroup: Boolean,
                                isArchived: Boolean,
                                isExternallyShared: Boolean,
                                isReadOnly: Boolean,
                                isOrgShared: Boolean
                              ) {
  def isDm: Boolean = isSelfDm || isOtherDm
}
