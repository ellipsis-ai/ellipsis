package models.behaviors.linked_github_repo

import models.behaviors.behaviorgroup.BehaviorGroup
import slick.dbio.DBIO

import scala.concurrent.Future

trait LinkedGithubRepoService {

  def maybeFor(group: BehaviorGroup): Future[Option[LinkedGithubRepo]]

  def ensureLinkAction(group: BehaviorGroup, owner: String, repo: String, maybeCurrentBranch: Option[String]): DBIO[LinkedGithubRepo]

  def ensureLink(group: BehaviorGroup, owner: String, repo: String, maybeCurrentBranch: Option[String]): Future[LinkedGithubRepo]

  def maybeSetCurrentBranch(group: BehaviorGroup, branch: String): Future[Option[LinkedGithubRepo]]

  def maybeSetCurrentBranch(maybeGroup: Option[BehaviorGroup], maybeBranch: Option[String]): Future[Option[LinkedGithubRepo]] = {
    (for {
      group <- maybeGroup
      branch <- maybeBranch
    } yield {
      maybeSetCurrentBranch(group, branch)
    }).getOrElse(Future.successful(None))
  }

  def unlink(group: BehaviorGroup): Future[Unit]

}
