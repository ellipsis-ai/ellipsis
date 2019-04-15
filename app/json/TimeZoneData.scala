package json

case class TimeZoneData(
                             tzName: String,
                             formattedName: Option[String],
                             currentOffset: Int
                           )
