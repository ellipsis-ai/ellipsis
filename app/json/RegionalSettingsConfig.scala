package json

case class RegionalSettingsConfig(
                                   containerId: String,
                                   csrfToken: Option[String],
                                   isAdmin: Boolean,
                                   teamId: String,
                                   teamTimeZone: Option[String],
                                   teamTimeZoneName: Option[String],
                                   teamTimeZoneOffset: Option[Int]
                                 )
