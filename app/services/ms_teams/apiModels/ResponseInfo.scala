package services.ms_teams.apiModels

case class ResponseInfo(
                         `type`: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationAccount,
                         recipient: Option[MessageParticipantInfo],
                         text: Option[String],
                         textFormat: Option[String],
                         replyToId: Option[String],
                         attachments: Option[Seq[Attachment]],
                         entities: Option[Seq[MentionEntity]]
                       )

object ResponseInfo {

  def newForMessage(
                     from: MessageParticipantInfo,
                     conversation: ConversationAccount,
                     recipient: MessageParticipantInfo,
                     text: String,
                     textFormat: String,
                     maybeReplyToId: Option[String],
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
      Some(recipient),
      Some(text),
      Some(textFormat),
      maybeReplyToId,
      attachments,
      Some(entities)
    )
  }

  def newForTyping(
                    from: MessageParticipantInfo,
                    conversation: ConversationAccount,
                    recipient: MessageParticipantInfo
                  ): ResponseInfo = {
    ResponseInfo(
      "typing",
      from,
      conversation,
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

}
