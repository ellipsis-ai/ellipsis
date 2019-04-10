package json

import java.time.OffsetDateTime

case class RecurrenceValidationData(recurrenceData: ScheduledActionRecurrenceData, nextRun: Option[OffsetDateTime])
