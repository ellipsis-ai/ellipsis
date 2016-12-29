package models

import drivers.SlickPostgresDriver.api._
import org.joda.time.DateTimeZone

object MappedColumnTypeImplicits {

  implicit val dateTimeZoneColumnType = MappedColumnType.base[DateTimeZone, String](
    { tz => tz.getID }, { str =>
      try {
        DateTimeZone.forID(str)
      } catch {
        case e: IllegalArgumentException => null
      }
    }
  )

}
