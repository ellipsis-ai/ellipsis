package models.loggedevent

import drivers.SlickPostgresDriver.api._

object LoggedEventQueries {

  val all = TableQuery[LoggedEventsTable]

}
