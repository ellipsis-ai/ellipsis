package services.ms_teams.apiModels

case class NewPrivateMessageInfo(
                                  bot: MessageParticipantInfo,
                                  members: Seq[DirectoryObject],
                                  channelData: ChannelDataInfo
                                )
