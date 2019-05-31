package models.behaviors.form

import drivers.SlickPostgresDriver.api._

object FormQueries {

  def all = TableQuery[FormsTable]

  def uncompiledFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
