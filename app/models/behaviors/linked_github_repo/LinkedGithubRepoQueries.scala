package models.behaviors.linked_github_repo

import drivers.SlickPostgresDriver.api._

object LinkedGithubRepoQueries {

  val all = TableQuery[LinkedGithubReposTable]

  def uncompiledFindQuery(behaviorGroupId: Rep[String]) = {
    all.filter(_.behaviorGroupId === behaviorGroupId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
