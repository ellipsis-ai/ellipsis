package utils.github

import export.BehaviorGroupExporter
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import services._
import services.caching.CacheService
import utils.ShellEscaping

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.sys.process.{Process, ProcessLogger}
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
                       implicit val ec: ExecutionContext
                       ) {

  val team: Team = behaviorGroup.team
  val config: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  val parentPath: String = s"/tmp/ellipsis-git/${team.id}/${user.id}"
  val exportName: String = behaviorGroup.id
  val dirName: String = s"$parentPath/$exportName"

  val remoteUrl: String = ShellEscaping.escapeWithSingleQuotes(s"https://$repoAccessToken@github.com/$owner/$repoName.git")

  val escapedBranch: String = ShellEscaping.escapeWithSingleQuotes(branch)
  val escapedCommitMessage: String = ShellEscaping.escapeWithSingleQuotes(commitMessage)
  val escapedCommitterName: String = ShellEscaping.escapeWithSingleQuotes(committerInfo.name)
  val escapedCommitterEmail: String = ShellEscaping.escapeWithSingleQuotes(committerInfo.email)

  private def runCommand(cmd: String, maybeCreateException: Option[String => Exception]): Future[String] = {
    println(cmd)
    Future {
      blocking {
        val buffer = new StringBuilder()
        val processLogger = ProcessLogger(
          logText => buffer.append(s"$logText\n")
        )
        val exitValue = Process(Seq("bash", "-c", cmd)).!(processLogger)
        val output = buffer.mkString
        if (exitValue != 0) {
          maybeCreateException.foreach { createException =>
            throw createException(output)
          }
        }
        output
      }
    }
  }

  private def cloneRepo: Future[String] = {
    runCommand(
      s"mkdir -p $parentPath && cd $parentPath && rm -rf $exportName && git clone $remoteUrl $exportName",
      Some(message => GitCloneException(owner, repoName, message))
    )
  }

  private def ensureBranch: Future[String] = {
    runCommand(s"cd $dirName && git checkout -b $escapedBranch && git push origin $escapedBranch", None)
  }

  private def ensureBranchCheckedOut: Future[String] = {
    runCommand(s"cd $dirName && git checkout $escapedBranch", None)
  }

  private def pullLatest: Future[String] = {
    runCommand(s"cd $dirName && git pull origin $escapedBranch", Some(GitPullException.apply))
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

  private def push: Future[String] = {
    runCommand(
      raw"""cd $dirName && git add . && git -c user.name=$escapedCommitterName -c user.email=$escapedCommitterEmail commit -a -m $escapedCommitMessage && git push origin $escapedBranch""",
      Some((message) => GitPushException.fromMessage(escapedBranch, message))
    )
  }

  private def setGitSHA: Future[Unit] = {
    for {
      sha <- runCommand(
        raw"""cd $dirName && git rev-parse HEAD""",
        None
      )
      _ <- dataService.behaviorGroupVersionSHAs.maybeCreateFor(behaviorGroup, sha.trim)
    } yield {}
  }

  private def cleanUp: Future[String] = {
    runCommand(s"rm -rf $dirName", None)
  }

  def run: Future[Unit] = {
    for {
      _ <- cloneRepo
      _ <- ensureBranch
      _ <- ensureBranchCheckedOut
      _ <- pullLatest
      _ <- export
      _ <- push
      _ <- setGitSHA
      _ <- cleanUp
    } yield {}
  }

}
