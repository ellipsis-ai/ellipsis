package models.loggedevent

import scala.concurrent.Future

trait LoggedEventService {

  def log(event: LoggedEvent): Future[Unit]

}
