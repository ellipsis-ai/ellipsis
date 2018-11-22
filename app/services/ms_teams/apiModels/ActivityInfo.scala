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

  def responseUrlFor(conversationId: String, maybeActivityId: Option[String]): String = {
    s"$responseUrlBase/${conversationId}/activities/${maybeActivityId.getOrElse("")}"
  }

  val responseUrl: String = responseUrlFor(conversation.id, Some(id))


  val getPrivateConversationIdUrl: String = responseUrlBase

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

}
