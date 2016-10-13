package json

import models.team.Team
import models.behaviors.behaviorparameter.BehaviorBackedDataType
import org.joda.time.DateTime
import play.api.libs.json.Json

import Formatting._
import services.DataService

case class BehaviorBackedDataTypeData(
                                      teamId: String,
                                      dataTypeId: Option[String],
                                      functionBody: String,
                                      config: BehaviorBackedDataTypeConfig,
                                      importedId: Option[String],
                                      githubUrl: Option[String],
                                      knownEnvVarsUsed: Seq[String],
                                      createdAt: Option[DateTime]
                                    ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def copyForTeam(team: Team): BehaviorBackedDataTypeData = {
    copy(teamId = team.id)
  }

  def behaviorVersionData(dataService: DataService): BehaviorVersionData = {
    BehaviorVersionData.buildFor(
      teamId,
      None,
      functionBody,
      "",
      Seq(),
      Seq(),
      BehaviorConfig(None, config.aws, config.requiredOAuth2ApiConfigs, Some(config.name)),
      None,
      None,
      dataTypeId.map(id => BehaviorBackedDataTypeDataForBehavior(id, config.name)),
      createdAt,
      dataService
    )
  }
}

object BehaviorBackedDataTypeData {

  def buildFor(teamId: String,
               dataTypeId: Option[String],
               functionBody: String,
               config: BehaviorBackedDataTypeConfig,
               importedId: Option[String],
               githubUrl: Option[String],
               createdAt: Option[DateTime],
               dataService: DataService
              ): BehaviorBackedDataTypeData = {

    val knownEnvVarsUsed = config.knownEnvVarsUsed ++ dataService.environmentVariables.lookForInCode(functionBody)

    BehaviorBackedDataTypeData(
      teamId,
      dataTypeId,
      functionBody,
      config,
      importedId,
      githubUrl,
      knownEnvVarsUsed,
      createdAt
    )
  }

  def fromStrings(
                 teamId: String,
                 function: String,
                 config: String,
                 maybeGithubUrl: Option[String],
                 dataService: DataService
                 ): BehaviorBackedDataTypeData = {
    BehaviorBackedDataTypeData.buildFor(
      teamId,
      None,
      BehaviorVersionData.extractFunctionBodyFrom(function),
      Json.parse(config).validate[BehaviorBackedDataTypeConfig].get,
      importedId = None,
      maybeGithubUrl,
      createdAt = None,
      dataService
    )
  }
}

case class BehaviorBackedDataTypeDataForBehavior(id: String, name: String)

object BehaviorBackedDataTypeDataForBehavior {
  def from(dataType: BehaviorBackedDataType): BehaviorBackedDataTypeDataForBehavior = {
    BehaviorBackedDataTypeDataForBehavior(dataType.id, dataType.name)
  }
}
