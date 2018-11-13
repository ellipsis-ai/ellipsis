package services.ms_teams.apiModels

case class ResponseInfo(
                         `type`: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationInfo,
                         recipient: MessageParticipantInfo,
                         text: String,
                         replyToId: String
                       )
