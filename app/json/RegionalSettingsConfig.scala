package json

case class RegionalSettingsConfig(
                                   containerId: String,
                                   csrfToken: Option[String],
                                   teamId: String,
                                   teamTimeZone: Option[String],
                                   teamTimeZoneName: Option[String],
                                   teamTimeZoneOffset: Option[Int]
                                 )
