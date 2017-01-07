package models

import java.time.ZoneId

import drivers.SlickPostgresDriver.api._

object MappedColumnTypeImplicits {

  implicit val zoneIdColumnType = MappedColumnType.base[ZoneId, String](
    { tz => tz.getId }, { str =>
      try {
        ZoneId.of(str)
      } catch {
        case _: IllegalArgumentException => null
      }
    }
  )

}
