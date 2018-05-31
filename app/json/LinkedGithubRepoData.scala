package json

import models.behaviors.linked_github_repo.LinkedGithubRepo

case class LinkedGithubRepoData(owner: String, repo: String, currentBranch: String)

object LinkedGithubRepoData {
  def apply(owner: String, repo: String, maybeCurrentBranch: Option[String]): LinkedGithubRepoData = {
    LinkedGithubRepoData(owner, repo, maybeCurrentBranch.getOrElse("master"))
  }

  def from(repo: LinkedGithubRepo): LinkedGithubRepoData = {
    LinkedGithubRepoData(repo.owner, repo.repo, repo.maybeCurrentBranch)
  }
}
