package controllers.api.json

import java.time.OffsetDateTime

import models.behaviors.ActionArg

case class ScheduleActionResult(
                                 actionName: Option[String],
                                 trigger: Option[String],
                                 arguments: Option[Seq[ActionArg]],
                                 recurrence: String,
                                 firstRecurrence: Option[OffsetDateTime],
                                 secondRecurrence: Option[OffsetDateTime],
                                 useDM: Boolean,
                                 channel: String
                               )
