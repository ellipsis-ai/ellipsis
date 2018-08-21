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
  val confirmation: String = s"You typed:\n>${commandText}"
}
