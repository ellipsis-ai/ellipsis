package utils.github

import export.BehaviorGroupExporter
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import play.api.Configuration
import services._

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.sys.process.{Process, ProcessLogger}

case class EnsureGitRepoDirException(message: String) extends Exception {
  override def getMessage(): String = s"Can't initialize git repo: $message"
}
case class GitPullException(message: String) extends Exception {
  override def getMessage(): String = s"Can't pull git repo: $message"
}
case class ExportForPushException(message: String) extends Exception {
  override def getMessage(): String = s"Can't export: $message"
}
case class GitPushException(message: String) extends Exception {
  override def getMessage(): String = s"Can't push git repo: $message"
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

  val remoteUrl: String = s"https://$repoAccessToken@github.com/$owner/$repoName.git"

  private def runCommand(cmd: String, maybeCreateException: Option[String => Exception]): Future[Unit] = {
    Future {
      blocking {
        val buffer = new StringBuilder()
        val processLogger = ProcessLogger(
          _ => {},
          logText => buffer.append(logText)
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

  private def pullRepo: Future[Unit] = {
    runCommand(s"mkdir -p $parentPath && cd $parentPath && rm -rf $exportName && git clone $remoteUrl $exportName && cd $exportName && git checkout $branch", Some(GitPullException.apply))
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
    runCommand(raw"""cd $dirName && git -c user.name='${committerInfo.name}' -c user.email='${committerInfo.email}' commit -a -m "$commitMessage" && git push origin $branch""", Some(GitPushException.apply))
  }

  private def cleanUp: Future[Unit] = {
    runCommand(s"rm -rf $dirName", None)
  }

  def run: Future[Unit] = {
    for {
      _ <- pullRepo
      _ <- export
      _ <- push
      _ <- cleanUp
    } yield {}
  }

}
