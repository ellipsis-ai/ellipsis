package services.ms_teams.apiModels

case class ChannelDataChannel(
                              id: String,
                              name: Option[String]
                            ) {
  def idWithoutMessage: String = """;messageid=.*$""".r.replaceAllIn(id, "")
}
