package json

import play.api.libs.json.Json

object Formatting {

  implicit val behaviorParameterReads = Json.reads[BehaviorParameterData]
  implicit val behaviorParameterWrites = Json.writes[BehaviorParameterData]

  implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  implicit val awsConfigReads = Json.reads[AWSConfigData]
  implicit val awsConfigWrites = Json.writes[AWSConfigData]

  implicit val oAuth2ApplicationReads = Json.reads[OAuth2ApplicationData]
  implicit val oAuth2ApplicationWrites = Json.writes[OAuth2ApplicationData]

  implicit val behaviorConfigReads = Json.reads[BehaviorConfig]
  implicit val behaviorConfigWrites = Json.writes[BehaviorConfig]

  implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  implicit val behaviorReads = Json.reads[BehaviorData]
  implicit val behaviorWrites = Json.writes[BehaviorData]

  implicit val behaviorCategoryReads = Json.reads[BehaviorCategory]
  implicit val behaviorCategoryWrites = Json.writes[BehaviorCategory]

  implicit val installedBehaviorReads = Json.reads[InstalledBehaviorData]
  implicit val installedBehaviorWrites = Json.writes[InstalledBehaviorData]

  implicit val environmentVariableReads = Json.reads[EnvironmentVariableData]
  implicit val environmentVariableWrites = Json.writes[EnvironmentVariableData]

  implicit val environmentVariablesReads = Json.reads[EnvironmentVariablesData]
  implicit val environmentVariablesWrites = Json.writes[EnvironmentVariablesData]

}
