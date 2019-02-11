package services.ms_teams.apiModels

case class ActivityInfo(
                         id: String,
                         serviceUrl: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationAccount,
                         recipient: MessageParticipantInfo,
                         text: Option[String],
                         channelData: ChannelDataInfo
                       ) extends EventInfo {

  val maybeId: Option[String] = Some(id)

  private val responseUrlBase = s"$serviceUrl/v3/conversations"

  def responseUrlFor(conversationId: String, maybeActivityId: Option[String]): String = {
    s"$responseUrlBase/${conversationId}/activities/${maybeActivityId.getOrElse("")}"
  }

  val responseUrl: String = responseUrlFor(conversation.id, Some(id))
  val maybeResponseUrl: Option[String] = Some(responseUrl)

  val getPrivateConversationIdUrl: String = responseUrlBase

  val conversationType: String = conversation.conversationType

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

  val channel: String = conversation.id
  val channelName: Option[String] = conversation.name

  val userIdForContext: String = from.id
  val maybeUserParticipant: Option[MessageParticipantInfo] = Some(from)
  val aadObjectId: Option[String] = from.aadObjectId
  val botUserIdForContext: String = recipient.id
  val botParticipant: MessageParticipantInfo = recipient

  val isDirectMessage: Boolean = conversation.conversationType == "personal"
  val isPublicChannel: Boolean = conversation.conversationType == "channel"

}
