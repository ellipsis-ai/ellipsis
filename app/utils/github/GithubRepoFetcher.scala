package utils.github

import models.team.Team

trait GithubRepoFetcher[T] extends GithubFetcher[T] {

  val owner: String
  val repoName: String
  val team: Team
  val maybeBranch: Option[String]

  val cacheKey: String = s"github_${owner}_${repoName}_${branch}"

  val branch: String = maybeBranch.getOrElse("master")

}
