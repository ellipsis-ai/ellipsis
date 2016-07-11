package models.bots

import org.joda.time.DateTime

case class TimeEvent(time: DateTime) extends Event {
  val context: TimeContext = new TimeContext()
}
