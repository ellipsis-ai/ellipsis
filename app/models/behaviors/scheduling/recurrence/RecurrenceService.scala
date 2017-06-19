package models.behaviors.scheduling.recurrence

import java.time.ZoneId

import scala.concurrent.Future

trait RecurrenceService {

  def save(recurrence: Recurrence): Future[Recurrence]

  def maybeCreateFromText(text: String, defaultTimeZone: ZoneId): Future[Option[Recurrence]]

  def delete(recurrenceId: String): Future[Boolean]

}
