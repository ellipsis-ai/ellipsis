package controllers.api.json

case class ScheduleResult(
                           scheduled: Option[ScheduleActionResult],
                           unscheduled: Option[Seq[ScheduleActionResult]])
