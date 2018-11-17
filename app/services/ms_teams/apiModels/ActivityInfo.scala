package services.ms_teams.apiModels

case class ActivityInfo(
                         id: String,
                         serviceUrl: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationInfo,
                         recipient: MessageParticipantInfo,
                         text: Option[String],
                         channelData: ChannelDataInfo
                       ) {
  val responseUrl: String = s"$serviceUrl/v3/conversations/${conversation.id}/activities/${id}"

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)
}
