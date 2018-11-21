package services.ms_teams.apiModels

case class ActivityInfo(
                         id: String,
                         serviceUrl: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationAccount,
                         recipient: MessageParticipantInfo,
                         text: Option[String],
                         channelData: ChannelDataInfo
                       ) {

  private val responseUrlBase = s"$serviceUrl/v3/conversations"

  val responseUrl: String = s"$responseUrlBase/${conversation.id}/activities/${id}"

  val privateMessageUrl: String = responseUrlBase

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)
}
