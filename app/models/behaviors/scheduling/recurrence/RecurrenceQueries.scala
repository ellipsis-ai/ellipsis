package models.behaviors.scheduling.recurrence

import drivers.SlickPostgresDriver.api._

object RecurrenceQueries {

  val all = TableQuery[RecurrencesTable]

  def uncompiledFindRawQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findRawQuery = Compiled(uncompiledFindRawQuery _)

}
