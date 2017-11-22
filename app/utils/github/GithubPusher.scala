package utils.github

import export.BehaviorGroupExporter
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import play.api.Configuration
import services._
import utils.ShellEscaping

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.matching.Regex

trait GitCommandException extends Exception

case class EnsureGitRepoDirException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error initializing git repo: $message"
}
case class GitCloneException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error cloning git repo: $message"
}
case class GitPullException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error pulling from GitHub: $message"
}
case class ExportForPushException(message: String) extends GitCommandException {
  override def getMessage: String = s"Error exporting skill: $message"
}
case class GitPushException(branch: String, message: String) extends GitCommandException {
  private val nothingToCommitRegex: Regex = """nothing to commit, working directory clean\s*\Z""".r
  override def getMessage: String = {
    val errorDetails = if (nothingToCommitRegex.findFirstIn(message).isDefined) {
      s"branch $branch has no changes to commit"
    } else {
      message.trim
    }
    s"Error pushing to GitHub: $errorDetails"
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

  val parentPath: String = s"/tmp/ellipsis-git/${team.id}/${user.id}"
  val exportName: String = behaviorGroup.id
  val dirName: String = s"$parentPath/$exportName"

  val remoteUrl: String = ShellEscaping.escapeWithSingleQuotes(s"https://$repoAccessToken@github.com/$owner/$repoName.git")

  val escapedBranch: String = ShellEscaping.escapeWithSingleQuotes(branch)
  val escapedCommitMessage: String = ShellEscaping.escapeWithSingleQuotes(commitMessage)
  val escapedCommitterName: String = ShellEscaping.escapeWithSingleQuotes(committerInfo.name)
  val escapedCommitterEmail: String = ShellEscaping.escapeWithSingleQuotes(committerInfo.email)

  private def runCommand(cmd: String, maybeCreateException: Option[String => Exception]): Future[Unit] = {
    println(cmd)
    Future {
      blocking {
        val buffer = new StringBuilder()
        val processLogger = ProcessLogger(
          logText => buffer.append(s"$logText\n")
        )
        val exitValue = Process(Seq("bash", "-c", cmd)).!(processLogger)
        if (exitValue != 0) {
          maybeCreateException.foreach { createException =>
            throw createException(buffer.mkString)
          }
        }
      }
    }
  }

  private def cloneRepo: Future[Unit] = {
    runCommand(s"mkdir -p $parentPath && cd $parentPath && rm -rf $exportName && git clone $remoteUrl $exportName", Some(GitCloneException.apply))
  }

  private def ensureBranch: Future[Unit] = {
    runCommand(s"cd $dirName && git checkout -b $escapedBranch && git push origin $escapedBranch", None)
  }

  private def ensureBranchCheckedOut: Future[Unit] = {
    runCommand(s"cd $dirName && git checkout $escapedBranch", None)
  }

  private def pullLatest: Future[Unit] = {
    runCommand(s"cd $dirName && git pull origin $escapedBranch", Some(GitPullException.apply))
  }

  private def export: Future[Unit] = {
    for {
      maybeExporter <- BehaviorGroupExporter.maybeFor(behaviorGroup.id, user, dataService, Some(parentPath), Some(exportName))
    } yield {
      maybeExporter.map { exporter =>
        exporter.writeFiles()
      }.getOrElse {
        throw ExportForPushException("Couldn't export skill")
      }
    }
  }

  private def push: Future[Unit] = {
    runCommand(
      raw"""cd $dirName && git add . && git -c user.name=$escapedCommitterName -c user.email=$escapedCommitterEmail commit -a -m $escapedCommitMessage && git push origin $escapedBranch""",
      Some((message) => GitPushException(escapedBranch, message))
    )
  }

  private def cleanUp: Future[Unit] = {
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
      _ <- cleanUp
    } yield {}
  }

}
