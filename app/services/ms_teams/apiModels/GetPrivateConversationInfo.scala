package services.ms_teams.apiModels

case class GetPrivateConversationInfo(
                                      bot: MessageParticipantInfo,
                                      members: Seq[DirectoryObject],
                                      channelData: ChannelDataInfo
                                    )
