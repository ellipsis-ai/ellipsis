package models.accounts.github.profile

import drivers.SlickPostgresDriver.api._

object GithubProfileQueries {

  val all = TableQuery[GithubProfilesTable]

  private def uncompiledFindQuery(providerId: Rep[String], providerKey: Rep[String]) = {
    all.filter(_.providerId === providerId).filter(_.providerKey === providerKey)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
