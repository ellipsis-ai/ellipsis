package json

case class LinkedGithubRepoData(owner: String, repo: String, currentBranch: String)

object LinkedGithubRepoData {
  def apply(owner: String, repo: String, maybeCurrentBranch: Option[String]): LinkedGithubRepoData = {
    LinkedGithubRepoData(owner, repo, maybeCurrentBranch.getOrElse("master"))
  }
}
