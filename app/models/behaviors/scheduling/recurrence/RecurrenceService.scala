package models.behaviors.scheduling.recurrence

import java.time.ZoneId

import slick.dbio.DBIO

import scala.concurrent.Future

trait RecurrenceService {

  def saveAction(recurrence: Recurrence): DBIO[Recurrence]

  def save(recurrence: Recurrence): Future[Recurrence]

  def maybeCreateFromText(text: String, defaultTimeZone: ZoneId): Future[Option[Recurrence]]

  def deleteAction(recurrenceId: String): DBIO[Boolean]

  def delete(recurrenceId: String): Future[Boolean]

}
