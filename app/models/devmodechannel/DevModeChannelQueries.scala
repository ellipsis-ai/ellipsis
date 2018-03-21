package models.devmodechannel

import drivers.SlickPostgresDriver.api._

object DevModeChannelQueries {

  def all = TableQuery[DevModeChannelsTable]

  def uncompiledFindQuery(context: Rep[String], channel: Rep[String], teamId: Rep[String]) = {
    all.
      filter(_.context === context).
      filter(_.channel === channel).
      filter(_.teamId === teamId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
