package models.accounts.slack.profile

import slick.driver.PostgresDriver.api._

object SlackProfileQueries {

  val all = TableQuery[SlackProfileTable]

  private def uncompiledFindSlackProfile(providerId: Rep[String], providerKey: Rep[String]) = {
    all.filter(_.providerId === providerId).filter(_.providerKey === providerKey)
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
