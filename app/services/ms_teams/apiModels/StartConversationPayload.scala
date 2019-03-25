package services.ms_teams.apiModels

case class MemberDetails(id: String, name: String)

object MemberDetails {
  def fromParticipant(participant: MessageParticipantInfo): MemberDetails = MemberDetails(participant.id, participant.name)
}

case class StartConversationPayload(
                                      bot: MemberDetails,
                                      isGroup: Boolean,
                                      members: Seq[MemberDetails],
                                      channelData: ChannelDataInfo
                                    )
