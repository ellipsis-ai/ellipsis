package models.behaviors.linked_github_repo

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class LinkedGithubReposTable(tag: Tag) extends Table[LinkedGithubRepo](tag, "linked_github_repos") {

  def owner = column[String]("owner")
  def repo = column[String]("repo")
  def behaviorGroupId = column[String]("group_id")
  def maybeCurrentBranch = column[Option[String]]("current_branch")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (owner, repo, behaviorGroupId, maybeCurrentBranch, createdAt) <>
    ((LinkedGithubRepo.apply _).tupled, LinkedGithubRepo.unapply _)
}

class LinkedGithubRepoServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService],
                                            implicit val ec: ExecutionContext
                                          ) extends LinkedGithubRepoService {

  def dataService = dataServiceProvider.get

  import LinkedGithubRepoQueries._

  def maybeForAction(group: BehaviorGroup): DBIO[Option[LinkedGithubRepo]] = {
    findQuery(group.id).result.map(_.headOption)
  }

  def maybeFor(group: BehaviorGroup): Future[Option[LinkedGithubRepo]] = {
    dataService.run(maybeForAction(group))
  }

  def ensureLink(group: BehaviorGroup, owner: String, repo: String, maybeCurrentBranch: Option[String]): Future[LinkedGithubRepo] = {
    val action = for {
      maybeExisting <- maybeForAction(group)
      maybeAlreadyLinked <- maybeExisting.map { existing =>
        if (existing.behaviorGroupId == group.id && existing.owner == owner && existing.repo == repo) {
          DBIO.successful(Some(existing))
        } else {
          unlinkAction(group).map(_ => None)
        }
      }.getOrElse(DBIO.successful(None))
      linked <- maybeAlreadyLinked.map { existing =>
        val updated = existing.copy(owner = owner, repo = repo, maybeCurrentBranch = maybeCurrentBranch)
        findQuery(group.id).update(updated).map(_ => updated)
      }.getOrElse {
        val newInstance = LinkedGithubRepo(owner, repo, group.id, maybeCurrentBranch, OffsetDateTime.now)
        (all += newInstance).map(_ => newInstance)
      }
    } yield linked

    dataService.run(action)
  }

  def maybeSetCurrentBranch(group: BehaviorGroup, branch: String): Future[Option[LinkedGithubRepo]] = {
    maybeFor(group).flatMap { maybeExisting =>
      maybeExisting.map { existing =>
        ensureLink(group, existing.owner, existing.repo, Some(branch)).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def unlinkAction(group: BehaviorGroup): DBIO[Unit] = {
    findQuery(group.id).delete.map(_ => {})
  }

  def unlink(group: BehaviorGroup): Future[Unit] = {
    dataService.run(unlinkAction(group))
  }

}
