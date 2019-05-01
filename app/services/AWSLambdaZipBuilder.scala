package services

import java.io.{File, PrintWriter}

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameter, FileType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.library.LibraryVersion
import play.api.Logger
import services.AWSLambdaConstants._
import utils.RequiredModulesInCode

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.reflect.io.Path
import scala.sys.process.{Process, ProcessLogger}

case class AWSLambdaZipBuilder(
                                groupVersion: BehaviorGroupVersion,
                                behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                                libraries: Seq[LibraryVersion],
                                apiConfigInfo: ApiConfigInfo
                              ) {

  val functionName: String = groupVersion.functionName
  val dirName: String = groupVersion.dirName
  val zipFileName: String = s"${dirName}.zip"

  private def writeFileNamed(path: String, content: String) = {
    val writer = new PrintWriter(new File(path))
    writer.write(content)
    writer.close()
  }

  private def hasFileParams(behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])]): Boolean = {
    behaviorVersionsWithParams.exists { case(_, params) => params.exists(_.input.paramType == FileType) }
  }

  private def requiredModulesForFileParams(behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])]): Seq[String] = {
    if (hasFileParams(behaviorVersionsWithParams)) {
      Seq("request")
    } else {
      Seq()
    }
  }

  def build(implicit ec: ExecutionContext): Future[Unit] = {

    val path = Path(dirName)
    path.createDirectory()

    writeFileNamed(s"$dirName/index.js", AWSLambdaIndexCodeBuilder(behaviorVersionsWithParams, apiConfigInfo).build)

    val behaviorVersionsDirName = s"$dirName/${BehaviorVersion.dirName}"
    Path(behaviorVersionsDirName).createDirectory()
    behaviorVersionsWithParams.foreach { case(behaviorVersion, params) =>
      writeFileNamed(s"$dirName/${behaviorVersion.jsName}", AWSLambdaBehaviorCodeBuilder(behaviorVersion, params, isForExport = false).build)
    }

    if (hasFileParams(behaviorVersionsWithParams)) {
      writeFileNamed(s"$dirName/$FETCH_FUNCTION_FOR_FILE_PARAM_NAME.js", FETCH_FUNCTION_FOR_FILE_PARAM)
    }

    libraries.foreach { ea =>
      writeFileNamed(s"$dirName/${ea.jsName}", ea.code)
    }

    val requiredModulesForBehaviorVersions = RequiredModulesInCode.requiredModulesIn(behaviorVersionsWithParams.map(_._1), libraries, includeLibraryRequires = true)
    val requiredModules = (requiredModulesForBehaviorVersions ++ requiredModulesForFileParams(behaviorVersionsWithParams)).distinct
    val logger = ProcessLogger((out) => Logger.info(out), (err) => Logger.error(err))
    for {
      _ <- if (requiredModules.isEmpty) {
        Future.successful({})
      } else {
        Future {
          blocking(
            Process(Seq("bash", "-c", s"cd $dirName && npm --loglevel=error init -f && npm --loglevel=error install --no-package-lock --target_platform=linux --target_libc=glibc ${requiredModules.mkString(" ")}"), None, "HOME" -> "/tmp").!(logger)
          )
        }
      }
      _ <- Future {
        blocking(
          Process(Seq("bash","-c",s"cd $dirName && zip -q -r $zipFileName *")).!(logger)
        )
      }
    } yield {}

  }

}
