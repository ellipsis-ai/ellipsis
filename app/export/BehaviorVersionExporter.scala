package export

import json._
import json.Formatting._
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json.Json
import services.DataService

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

  val dirName = {
    val behaviorType = if (behaviorVersion.behavior.isDataType) { "data_types" } else { "actions" }
    s"$parentPath/$behaviorType/${behaviorVersion.id}"
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

}

object BehaviorVersionExporter {

  def maybeFor(behaviorId: String, user: User, parentPath: String, dataService: DataService): Future[Option[BehaviorVersionExporter]] = {
    for {
      maybeBehavior <- dataService.behaviors.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        dataService.behaviors.maybeCurrentVersionFor(behavior)
      }.getOrElse(Future.successful(None))
      maybeFunction <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorVersions.maybeFunctionFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService, Some(behaviorId))
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
          versionData.responseTemplate,
          parentPath
        )
      }
    }
  }
}
