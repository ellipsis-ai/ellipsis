package utils

case class SlashCommandInfo(
                             command: String,
                             text: String,
                             responseUrl: String,
                             userId: String,
                             teamId: String,
                             channelId: String
                           ) {

  val commandText: String = s"${command} ${text}"
  val confirmation: String = s"Sending the following to the bot: `${text}`"
}
