package services.ms_teams.apiModels

case class ResponseInfo(
                         `type`: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationInfo,
                         recipient: MessageParticipantInfo,
                         text: String,
                         textFormat: String,
                         replyToId: String,
                         attachments: Option[Seq[Attachment]],
                         entities: Option[Seq[MentionEntity]]
                       )

object ResponseInfo {

  def newForMessage(
                     from: MessageParticipantInfo,
                     conversation: ConversationInfo,
                     recipient: MessageParticipantInfo,
                     text: String,
                     textFormat: String,
                     replyToId: String,
                     attachments: Option[Seq[Attachment]]
                   ): ResponseInfo = {
    val entities = Seq(from.toMentionEntity, recipient.toMentionEntity).flatMap{ ea =>
      if (text.contains(ea.text)) {
        Some(ea)
      } else {
        None
      }
    }
    ResponseInfo(
      "message",
      from,
      conversation,
      recipient,
      text,
      textFormat,
      replyToId,
      attachments,
      Some(entities)
    )
  }

}
