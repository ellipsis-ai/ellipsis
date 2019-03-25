package services.ms_teams.apiModels

sealed trait Entity {
  val `type`: String
}

case class MentionEntity(
                        mentioned: ChannelAccount,
                        text: String
                        ) extends Entity {
  val `type`: String = "mention"
}
