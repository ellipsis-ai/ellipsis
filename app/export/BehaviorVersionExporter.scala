package export

import java.io.{File, PrintWriter}

import json._
import json.Formatting._
import models.accounts.user.User
import models.bots.{BehaviorQueries, BehaviorVersion}
import play.api.libs.json.Json
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.io.Path
import scala.sys.process.Process

case class BehaviorVersionExporter(
                                    behaviorVersion: BehaviorVersion,
                                    maybeFunction: Option[String],
                                    paramsData: Seq[BehaviorParameterData],
                                    triggersData: Seq[BehaviorTriggerData],
                                    config: BehaviorConfig,
                                    responseTemplate: String) {

  def functionString: String = maybeFunction.getOrElse("")
  def paramsString: String = Json.prettyPrint(Json.toJson(paramsData))
  def triggersString: String = Json.prettyPrint(Json.toJson(triggersData))
  def configString: String = Json.prettyPrint(Json.toJson(config))

  private def dirName = s"/tmp/exports/${behaviorVersion.id}"
  private def zipFileName = s"$dirName.zip"

  private def writeFileFor(filename: String, content: String): Unit = {
    val writer = new PrintWriter(new File(s"$dirName/$filename"))
    writer.write(content)
    writer.close()
  }

  private def createZip: Unit = {
    val path = Path(dirName)
    path.createDirectory()

    writeFileFor("function.js", functionString)
    writeFileFor("triggers.json", triggersString)
    writeFileFor("params.json", paramsString)
    writeFileFor("response.md", responseTemplate)
    writeFileFor("config.json", configString)

    Process(Seq("bash","-c",s"cd $dirName && zip -r $zipFileName *")).!
  }

  def getZipFile: File = {
    createZip
    new File(zipFileName)
  }

}

object BehaviorVersionExporter {

  def maybeFor(behaviorId: String, user: User): DBIO[Option[BehaviorVersionExporter]] = {
    for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        behavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      maybeFunction <- maybeBehaviorVersion.map { behaviorVersion =>
        behaviorVersion.maybeFunction
      }.getOrElse(DBIO.successful(None))
      maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, Some(behaviorId))
    } yield {
      for {
        behaviorVersion <- maybeBehaviorVersion
        function <- maybeFunction
        versionData <- maybeVersionData
      } yield {
        // we don't want to export the team-specific application, but we want to keep the scope
        val requiredOAuth2ApiConfigsForExport = versionData.config.requiredOAuth2ApiConfigs.map { configs =>
          configs.map { ea =>
            val maybeScope = ea.application.flatMap(_.scope)
            ea.copy(application = None, recommendedScope = maybeScope)
          }
        }
        val configForExport = versionData.config.copy(requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsForExport)
        BehaviorVersionExporter(
          behaviorVersion,
          maybeFunction,
          versionData.params,
          versionData.triggers,
          configForExport,
          versionData.responseTemplate)
      }
    }
  }
}
