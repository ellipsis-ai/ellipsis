package models.accounts.slack.profile

import drivers.SlickPostgresDriver.api._

object SlackProfileQueries {

  val all = TableQuery[SlackProfileTable]

  private def uncompiledFindSlackProfileForLoginInfo(providerId: Rep[String], providerKey: Rep[String]) = {
    all.filter(_.providerId === providerId).filter(_.providerKey === providerKey)
  }
  val findSlackProfileForLoginInfo = Compiled(uncompiledFindSlackProfileForLoginInfo _)

  private def uncompiledFindSlackProfile(providerId: Rep[String], providerKey: Rep[String], teamId: Rep[String]) = {
    uncompiledFindSlackProfileForLoginInfo(providerId, providerKey).filter(_.teamId === teamId)
  }
  val findSlackProfileQuery = Compiled(uncompiledFindSlackProfile _)

  def uncompiledCountForTeam(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId).length
  }
  val countForTeam = Compiled(uncompiledCountForTeam _)

  def uncompiledAllForQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
