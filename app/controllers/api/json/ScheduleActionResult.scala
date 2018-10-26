package controllers.api.json

import java.time.OffsetDateTime

case class ScheduleActionResult(
                                 actionName: Option[String],
                                 trigger: Option[String],
                                 arguments: Option[Seq[RunActionArgumentInfo]],
                                 recurrence: String,
                                 firstRecurrence: Option[OffsetDateTime],
                                 secondRecurrence: Option[OffsetDateTime],
                                 useDM: Boolean,
                                 channel: String
                               )
