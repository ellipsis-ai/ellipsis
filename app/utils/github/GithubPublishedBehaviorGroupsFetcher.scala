package utils.github

import json._
import models.team.Team
import services.{DefaultServices, GithubService}

import scala.concurrent.{ExecutionContext, Future}

case class GithubPublishedBehaviorGroupsFetcher(
                                                  team: Team,
                                                  maybeBranch: Option[String],
                                                  alreadyInstalled: Seq[BehaviorGroupData],
                                                  githubService: GithubService,
                                                  services: DefaultServices,
                                                  implicit val ec: ExecutionContext
                                                ) {

  def result: Future[Seq[BehaviorGroupData]] = {
    GithubSkillCommitsFetcher(team, maybeBranch, alreadyInstalled, githubService, services, ec).result.flatMap { commits =>
      Future.sequence(commits.map { ea =>
        GithubSingleCommitFetcher(team, ea.owner, ea.repoName, ea.commitId, maybeBranch, None, githubService, services, ec).result
      })
    }
  }

}
