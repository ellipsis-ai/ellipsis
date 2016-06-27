package export

import java.io.{PrintWriter, File}
import json.EditorFormat.{BehaviorParameterData, BehaviorTriggerData, BehaviorVersionData}
import models.accounts.User
import models.bots.{BehaviorVersion, BehaviorQueries}
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
                                    responseTemplate: String) {

  def functionString: String = maybeFunction.getOrElse("")
  def paramsString: String = Json.prettyPrint(Json.toJson(paramsData))
  def triggersString: String = Json.prettyPrint(Json.toJson(triggersData))

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

    Process(Seq("bash","-c",s"cd $dirName && zip -r $zipFileName *")).!
  }

  def getZipFile: File = {
    createZip
    new File(zipFileName)
  }

  def exportString: String =
    s"""
       |//function.js:
       |
       |$functionString
       |
       |
       |//params.json:
       |$paramsString
       |
       |
       |//triggers.json:
       |$triggersString
       |
       |
       |//response.md:
       |$responseTemplate
       |
     """.stripMargin

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
      maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user)
    } yield {
      for {
        behaviorVersion <- maybeBehaviorVersion
        function <- maybeFunction
        versionData <- maybeVersionData
      } yield {
        BehaviorVersionExporter(behaviorVersion, maybeFunction, versionData.params, versionData.triggers, versionData.responseTemplate)
      }
    }
  }
}
