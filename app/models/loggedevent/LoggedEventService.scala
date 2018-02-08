package models.loggedevent

import slick.dbio.DBIO

import scala.concurrent.Future

trait LoggedEventService {

  def logAction(event: LoggedEvent): DBIO[Unit]

  def log(event: LoggedEvent): Future[Unit]

}
