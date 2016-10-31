package json

import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

object Formatting {

  implicit lazy val behaviorParameterTypeReads: Reads[BehaviorParameterTypeData] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "needsConfig").readNullable[Boolean] and
      (JsPath \ "behavior").lazyReadNullable(behaviorVersionReads)
    )(BehaviorParameterTypeData.apply _)

  implicit lazy val behaviorParameterTypeWrites: Writes[BehaviorParameterTypeData] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "needsConfig").writeNullable[Boolean] and
      (JsPath \ "behavior").lazyWriteNullable(behaviorVersionWrites)
    )(unlift(BehaviorParameterTypeData.unapply))

  implicit val behaviorParameterReads = Json.reads[BehaviorParameterData]
  implicit val behaviorParameterWrites = Json.writes[BehaviorParameterData]

  implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  implicit val awsConfigReads = Json.reads[AWSConfigData]
  implicit val awsConfigWrites = Json.writes[AWSConfigData]

  implicit val oAuth2ApiReads = Json.reads[OAuth2ApiData]
  implicit val oAuth2ApiWrites = Json.writes[OAuth2ApiData]

  implicit val oAuth2ApplicationReads = Json.reads[OAuth2ApplicationData]
  implicit val oAuth2ApplicationWrites = Json.writes[OAuth2ApplicationData]

  implicit val requiredOAuth2ApiConfigReads = Json.reads[RequiredOAuth2ApiConfigData]
  implicit val requiredOAuth2ApiConfigWrites = Json.writes[RequiredOAuth2ApiConfigData]

  implicit val behaviorConfigReads = Json.reads[BehaviorConfig]
  implicit val behaviorConfigWrites = Json.writes[BehaviorConfig]

  lazy implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  lazy implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

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

  implicit val apiTokenReads = Json.reads[APITokenData]
  implicit val apiTokenWrites = Json.writes[APITokenData]

  implicit val invocationLogEntryReads = Json.reads[InvocationLogEntryData]
  implicit val invocationLogEntryWrites = Json.writes[InvocationLogEntryData]

  implicit val invocationLogEntriesByDayReads = Json.reads[InvocationLogsByDayData]
  implicit val invocationLogEntriesByDayWrites = Json.writes[InvocationLogsByDayData]
}
