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
  val preamble: String = s"<@${userId}> triggered the command `${commandText}`"
  val confirmation: String = s"OK, running `${commandText}`"
}
