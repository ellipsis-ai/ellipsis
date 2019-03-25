package json

import java.time.OffsetDateTime

case class ScheduledActionValidatedRecurrenceData(recurrence: ScheduledActionRecurrenceData, nextRuns: Seq[Option[OffsetDateTime]])
