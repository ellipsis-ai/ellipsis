package json

import models.behaviors.linked_github_repo.LinkedGithubRepo
import utils.github.GithubUtils

case class LinkedGithubRepoData(owner: String, repo: String, currentBranch: String)

object LinkedGithubRepoData {
  def apply(owner: String, repo: String, maybeCurrentBranch: Option[String]): LinkedGithubRepoData = {
    LinkedGithubRepoData(owner, repo, maybeCurrentBranch.getOrElse("master"))
  }

  def from(repo: LinkedGithubRepo): LinkedGithubRepoData = {
    LinkedGithubRepoData(repo.owner, repo.repo, repo.maybeCurrentBranch)
  }

  def maybeFrom(data: BehaviorGroupData): Option[LinkedGithubRepoData] = {
    for {
      owner <- data.linkedGithubRepo.map(_.owner)
      name <- data.linkedGithubRepo.map(_.repo)
    } yield {
      LinkedGithubRepoData(owner, name, data.linkedGithubRepo.map(_.currentBranch))
    }
  }
}
