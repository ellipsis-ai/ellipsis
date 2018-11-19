package services.ms_teams.apiModels

case class MessageParticipantInfo(id: String, name: String) {
  def toMentionEntity: MentionEntity = {
    MentionEntity(ChannelAccount(id, name), s"<at>$name</at>")
  }
}
