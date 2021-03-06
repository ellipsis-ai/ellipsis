package utils.github

import java.io.File
import java.util

import export.BehaviorGroupExporter
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.HiddenFileFilter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors._
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.{RefSpec, UsernamePasswordCredentialsProvider}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import services._
import services.caching.CacheService

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

sealed trait GitCommandException extends Exception

object GitPushExceptionType extends Enumeration {
  type GitPushExceptionType = Value
  val NoChanges, Unknown = Value
}

case class EnsureGitRepoDirException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error initializing git repo: $message"
}
case class GitCloneException(owner: String, repo: String, message: String) extends GitCommandException {
  private val repoDoesntExistRegex = """Repository not found""".r
  override def getMessage: String = {
    val errorDetails = if (repoDoesntExistRegex.findFirstIn(message).isDefined) {
      s"Repo '$repo' doesn't exist for $owner"
    } else {
      message.trim
    }
    s"Error cloning git repo: $errorDetails"
  }
}
case class GitPullException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error pulling from GitHub: $message"
}
case class ExportForPushException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error exporting skill: $message"
}
case object NoMasterBranchException extends GitCommandException {
  override def getMessage: String = "No master branch yet. Try pushing to master first."
}
case class InvalidBranchNameException(message: String) extends GitCommandException {
  override def getMessage: String = message
}
case class GitPushException(exceptionType: GitPushExceptionType.Value, message: String, branch: String) extends GitCommandException {
  val details: JsObject = Json.obj("branch" -> branch)
  override def getMessage: String = message
}

object GitPushException {
  private val nothingToCommitRegex: Regex = """nothing to commit, working directory clean\s*\Z""".r

  def fromMessage(branch: String, originalMessage: String): GitPushException = {
    if (nothingToCommitRegex.findFirstIn(originalMessage).isDefined) {
      GitPushException(GitPushExceptionType.NoChanges, s"Warning: branch $branch has no changes from master to commit.", branch)
    } else {
      GitPushException(GitPushExceptionType.Unknown, s"Error pushing to GitHub: ${originalMessage.trim}", branch)
    }
  }
}

case class GithubPusher(
                       owner: String,
                       repoName: String,
                       branch: String,
                       commitMessage: String,
                       repoAccessToken: String,
                       committerInfo: GithubCommitterInfo,
                       behaviorGroup: BehaviorGroup,
                       user: User,
                       services: DefaultServices,
                       maybeRemoteUrl: Option[String],
                       implicit val ec: ExecutionContext
                       ) {
  val team: Team = behaviorGroup.team
  val exportName: String = behaviorGroup.id
  val parentPath: String = s"/tmp/ellipsis-git/${team.id}/${user.id}"
  val dirName: String = s"$parentPath/$exportName"

  val config: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  val remoteUrl: String = maybeRemoteUrl.getOrElse {
    s"https://github.com/$owner/$repoName.git"
  }

  private val credentialsProvider = new UsernamePasswordCredentialsProvider(repoAccessToken, "")

  private def parentDir: File = new File(parentPath)
  private def repoDir: File = new File(parentDir, exportName)

  private def cloneRepo: Git = {
    parentDir.mkdir()
    FileUtils.deleteDirectory(repoDir)
    try {
      Git.cloneRepository.
        setURI(remoteUrl).
        setCloneAllBranches(true).
        setDirectory(repoDir).
        setCredentialsProvider(credentialsProvider).
        call
    } catch {
      case e: GitAPIException => throw GitCloneException(owner, repoName, e.getMessage)
    }
  }

  private def ensureBranch(git: Git): Unit = {
    try {
      git.branchCreate.
        setName(branch).
        call
    } catch {
      case _: RefAlreadyExistsException => {}
      case _: RefNotFoundException => {
        if (branch != "master") throw NoMasterBranchException
      }
      case e: InvalidRefNameException => throw InvalidBranchNameException(e.getMessage)
    }
  }

  private def ensureBranchCheckedOut(git: Git): Unit = {
    try {
      git.checkout.
        setName(branch).
        call
    } catch {
      case e: RefNotFoundException => {}
    }
  }

  private def pullLatest(git: Git): Unit = {
    try {
      git.pull.
        setCredentialsProvider(credentialsProvider).
        setRemote("origin").
        setRemoteBranchName(branch).
        call
    } catch {
      case _: RefNotAdvertisedException => {} // brand new repo with no master branch pushed yet
      case e: GitAPIException => throw GitPullException(e.getMessage)
    }
  }

  private def deleteFiles: Unit = {
    val files = FileUtils.listFilesAndDirs(repoDir, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE)
    files.asScala.foreach { file =>
      if (file != repoDir) {
        FileUtils.deleteQuietly(file)
      }
    }
  }

  private def export: Future[Unit] = {
    for {
      maybeExporter <- BehaviorGroupExporter.maybeFor(behaviorGroup.id, user, dataService, cacheService, Some(parentPath), Some(exportName))
    } yield {
      maybeExporter.map { exporter =>
        exporter.writeFiles()
      }.getOrElse {
        throw ExportForPushException("Couldn't export skill")
      }
    }
  }

  private def updateOrRemoveExistingFiles(git: Git): Unit = {
    git.add.
      setUpdate(true).
      addFilepattern(".").
      call
  }

  private def addAllWithNewFiles(git: Git): Unit = {
    git.add.
      addFilepattern(".").
      call
  }

  private def push(git: Git): Unit = {
    try {
      updateOrRemoveExistingFiles(git)
      addAllWithNewFiles(git)
      git.commit.
        setAuthor(committerInfo.name, committerInfo.email).
        setMessage(commitMessage).
        call
      git.push.
        setCredentialsProvider(credentialsProvider).
        setRemote("origin").
        setRefSpecs(new RefSpec( s"$branch:$branch" ) ).
        call
    } catch {
      case e: GitAPIException => throw GitPushException.fromMessage(branch, e.getMessage)
    }
  }

  private def setGitSHA(git: Git): Future[Unit] = {
    val head = git.getRepository.resolve(Constants.HEAD)
    val commits = new util.ArrayList[RevCommit]()
    val iterator = git.log.add(head).setMaxCount(1).call.iterator

    while ( {
      iterator.hasNext
    }) commits.add(iterator.next)

    val sha = commits.get(0).toString
    dataService.behaviorGroupVersionSHAs.maybeCreateFor(behaviorGroup, sha.trim).map(_ => {})
  }

  private def cleanUp: Unit = {
    FileUtils.deleteDirectory(repoDir)
  }

  def run: Future[Unit] = {
    for {
      git <- Future.successful(cloneRepo)
      _ <- Future.successful(ensureBranch(git))
      _ <- Future.successful(ensureBranchCheckedOut(git))
      _ <- Future.successful(pullLatest(git))
      _ <- Future.successful(deleteFiles)
      _ <- export
      _ <- Future.successful(push(git))
      _ <- setGitSHA(git)
      _ <- Future.successful(cleanUp)
    } yield {}
  }

}
