package models.behaviors.linked_github_repo

import models.behaviors.behaviorgroup.BehaviorGroup

import scala.concurrent.Future

trait LinkedGithubRepoService {

  def maybeFor(group: BehaviorGroup): Future[Option[LinkedGithubRepo]]

  def link(group: BehaviorGroup, owner: String, repo: String): Future[LinkedGithubRepo]

  def unlink(group: BehaviorGroup): Future[Unit]

}
