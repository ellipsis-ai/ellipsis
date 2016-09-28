package export

import json._
import json.Formatting._
import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorBackedDataType
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorBackedDataTypeExporter(
                                          dataType: BehaviorBackedDataType,
                                          behaviorVersion: BehaviorVersion,
                                          maybeFunction: Option[String],
                                          config: BehaviorBackedDataTypeConfig
                                         ) extends Exporter {

  def functionString: String = maybeFunction.getOrElse("")
  def configString: String = Json.prettyPrint(Json.toJson(config))

  val exportId = behaviorVersion.id

  protected def writeFiles(): Unit = {
    writeFileFor("function.js", functionString)
    writeFileFor("config.json", configString)
  }

}

object BehaviorBackedDataTypeExporter {

  def maybeFor(dataTypeId: String, user: User, dataService: DataService): Future[Option[BehaviorBackedDataTypeExporter]] = {
    for {
      maybeDataType <- dataService.behaviorBackedDataTypes.find(dataTypeId, user)
      maybeBehaviorVersion <- maybeDataType.map { dataType =>
        dataService.behaviors.maybeCurrentVersionFor(dataType.behavior)
      }.getOrElse(Future.successful(None))
      maybeFunction <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorVersions.maybeFunctionFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeVersionData <- maybeDataType.map { dataType =>
        BehaviorVersionData.maybeFor(dataType.behavior.id, user, dataService, Some(dataType.behavior.id))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        dataType <- maybeDataType
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
        val behaviorConfigForExport = versionData.config.copy(requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsForExport)

        BehaviorBackedDataTypeExporter(
          dataType,
          behaviorVersion,
          maybeFunction,
          BehaviorBackedDataTypeConfig.buildFrom(dataType.name, behaviorConfigForExport)
        )
      }
    }
  }
}
