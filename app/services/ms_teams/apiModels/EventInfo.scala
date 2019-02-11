package services.ms_teams.apiModels

trait EventInfo {
  val serviceUrl: String
  val channel: String
  val channelName: Option[String]
  val channelData: ChannelDataInfo
  val userIdForContext: String
  val maybeUserParticipant: Option[MessageParticipantInfo]
  val aadObjectId: Option[String]
  val botUserIdForContext: String
  val botParticipant: MessageParticipantInfo

  val isDirectMessage: Boolean
  val isPublicChannel: Boolean

  val maybeResponseUrl: Option[String]
  val getPrivateConversationIdUrl: String

  val maybeId: Option[String]
  val conversationType: String
}
