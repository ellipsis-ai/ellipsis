package json

case class TeamTimeZoneData(
                             tzName: String,
                             formattedName: Option[String],
                             currentOffset: Int
                           )
