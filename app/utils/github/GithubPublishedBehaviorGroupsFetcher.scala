package utils.github

import json._
import models.team.Team
import play.api.Configuration
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubPublishedBehaviorGroupsFetcher(
                                                  team: Team,
                                                  maybeBranch: Option[String],
                                                  alreadyInstalled: Seq[BehaviorGroupData],
                                                  githubService: GithubService,
                                                  services: DefaultServices,
                                                  implicit val ec: ExecutionContext
                                                ) {

  val config: Configuration = services.configuration
  val branch: String = maybeBranch.getOrElse("master")

  val owner: String = "ellipsis-ai"
  val repoName: String = "behaviors"
  val token: String = config.get[String]("github.repoAccessToken")

  def result: Seq[BehaviorGroupData] = {
    val commits = GithubSkillCommitsFetcher(team, maybeBranch, alreadyInstalled, githubService, services, ec).result
    commits.map { ea =>
      GithubSingleCommitFetcher(team, ea.owner, ea.repoName, ea.commitId, maybeBranch, None, githubService, services, ec).result
    }
  }

}
