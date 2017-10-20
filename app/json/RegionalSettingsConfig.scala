package json

case class RegionalSettingsConfig(
                                   containerId: String,
                                   csrfToken: Option[String],
                                   teamId: String,
                                   teamTimeZone: Option[String]
                                 )
