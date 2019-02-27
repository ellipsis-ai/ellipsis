package services.ms_teams.apiModels

case class FirstMessageInfo(
                             from: MessageParticipantInfo,
                             recipient: MessageParticipantInfo,
                             channelData: ChannelDataInfo,
                             conversationType: String
                           ) extends EventInfo {

  val maybeId: Option[String] = None

  val maybeResponseUrl: Option[String] = None

  val serviceUrl: String = "https://smba.trafficmanager.net/amer/" // TODO: theoretically this should be saved away from a recent event
  val getPrivateConversationIdUrl: String = s"$serviceUrl/v3/conversations"

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

  val channel: String = channelData.channel.get.id
  val channelName: Option[String] = channelData.channel.flatMap(_.name)

  val userIdForContext: String = from.id
  val maybeUserParticipant: Option[MessageParticipantInfo] = Some(from)
  val aadObjectId: Option[String] = from.aadObjectId
  val botUserIdForContext: String = recipient.id
  val botParticipant: MessageParticipantInfo = recipient

  val isDirectMessage: Boolean = conversationType == "personal"
  val isPublicChannel: Boolean = conversationType == "channel"

}
