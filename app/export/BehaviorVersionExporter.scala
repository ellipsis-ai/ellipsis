package export

import json._
import json.Formatting._
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json.Json
import services.DataService
import utils.SafeFileName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorVersionExporter(
                                    behaviorVersion: BehaviorVersion,
                                    maybeFunction: Option[String],
                                    paramsData: Seq[BehaviorParameterData],
                                    triggersData: Seq[BehaviorTriggerData],
                                    config: BehaviorConfig,
                                    responseTemplate: String,
                                    parentPath: String
                                  ) extends Exporter {

  val fullPath = {
    val behaviorType = if (behaviorVersion.behavior.isDataType) { "data_types" } else { "actions" }
    val safeDirName = SafeFileName.forName(behaviorVersion.exportName)
    s"$parentPath/$behaviorType/$safeDirName"
  }

  def functionString: String = maybeFunction.getOrElse("")
  def paramsString: String = Json.prettyPrint(Json.toJson(paramsData))
  def triggersString: String = Json.prettyPrint(Json.toJson(triggersData))
  def configString: String = Json.prettyPrint(Json.toJson(config))

  protected def writeFiles(): Unit = {
    behaviorVersion.maybeDescription.foreach { desc =>
      writeFileFor("README", desc)
    }
    writeFileFor("function.js", functionString)
    writeFileFor("triggers.json", triggersString)
    writeFileFor("params.json", paramsString)
    writeFileFor("response.md", responseTemplate)
    writeFileFor("config.json", configString)
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorVersionExporter = {
    copy(paramsData = paramsData.map(_.copyForExport(groupExporter)))
  }

}

object BehaviorVersionExporter {

  def maybeFor(behaviorId: String, user: User, parentPath: String, dataService: DataService): Future[Option[BehaviorVersionExporter]] = {
    for {
      maybeBehavior <- dataService.behaviors.find(behaviorId, user).flatMap { maybeBehavior =>
        maybeBehavior.map { behavior =>
          dataService.behaviors.ensureExportIdFor(behavior).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        dataService.behaviors.maybeCurrentVersionFor(behavior)
      }.getOrElse(Future.successful(None))
      maybeFunction <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorVersions.maybeFunctionFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeExportId <- Future.successful(maybeBehavior.flatMap(_.maybeExportId))
      maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService, maybeExportId)
    } yield {
      for {
        behaviorVersion <- maybeBehaviorVersion
        versionData <- maybeVersionData
      } yield {
        BehaviorVersionExporter(
          behaviorVersion,
          maybeFunction,
          versionData.params,
          versionData.triggers,
          versionData.config.copyForExport,
          versionData.responseTemplate,
          parentPath
        )
      }
    }
  }
}
