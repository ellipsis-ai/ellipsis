package json

import play.api.libs.json._
import utils.CityInfo

object Formatting {

  implicit val behaviorParameterTypeReads = Json.reads[BehaviorParameterTypeData]
  implicit val behaviorParameterTypeWrites = Json.writes[BehaviorParameterTypeData]

  implicit val inputSavedAnswerReads = Json.reads[InputSavedAnswerData]
  implicit val inputSavedAnswerWrites = Json.writes[InputSavedAnswerData]

  implicit val inputReads = Json.reads[InputData]
  implicit val inputWrites = Json.writes[InputData]

  implicit val libraryVersionReads = Json.reads[LibraryVersionData]
  implicit val libraryVersionWrites = Json.writes[LibraryVersionData]

  implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  implicit val awsConfigReads = Json.reads[AWSConfigData]
  implicit val awsConfigWrites = Json.writes[AWSConfigData]

  implicit val oAuth2ApiReads = Json.reads[OAuth2ApiData]
  implicit val oAuth2ApiWrites = Json.writes[OAuth2ApiData]

  implicit val oAuth2ApplicationReads = Json.reads[OAuth2ApplicationData]
  implicit val oAuth2ApplicationWrites = Json.writes[OAuth2ApplicationData]

  implicit val oAuth2ApplicationListConfigWrites = Json.writes[OAuth2ApplicationListConfig]

  implicit val oAuth2ApplicationEditConfigWrites = Json.writes[OAuth2ApplicationEditConfig]

  implicit val requiredOAuth2ApiConfigReads = Json.reads[RequiredOAuth2ApiConfigData]
  implicit val requiredOAuth2ApiConfigWrites = Json.writes[RequiredOAuth2ApiConfigData]

  implicit val simpleTokenApiReads = Json.reads[SimpleTokenApiData]
  implicit val simpleTokenApiWrites = Json.writes[SimpleTokenApiData]

  implicit val requiredSimpleTokenApiReads = Json.reads[RequiredSimpleTokenApiData]
  implicit val requiredSimpleTokenApiWrites = Json.writes[RequiredSimpleTokenApiData]

  implicit val behaviorConfigReads = Json.reads[BehaviorConfig]
  implicit val behaviorConfigWrites = Json.writes[BehaviorConfig]

  lazy implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  lazy implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  implicit val behaviorReads = Json.reads[BehaviorData]
  implicit val behaviorWrites = Json.writes[BehaviorData]

  implicit val behaviorGroupReads = Json.reads[BehaviorGroupData]
  implicit val behaviorGroupWrites = Json.writes[BehaviorGroupData]

  implicit val behaviorGroupConfigReads = Json.reads[BehaviorGroupConfig]
  implicit val behaviorGroupConfigWrites = Json.writes[BehaviorGroupConfig]

  implicit val installedBehaviorReads = Json.reads[InstalledBehaviorGroupData]
  implicit val installedBehaviorWrites = Json.writes[InstalledBehaviorGroupData]

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

  implicit val scheduledActionDataReads = Json.reads[ScheduledActionData]
  implicit val scheduledActionDataWrites = Json.writes[ScheduledActionData]

  implicit val scheduledActionsDataReads = Json.reads[ScheduledActionsData]
  implicit val scheduledActionsDataWrites = Json.writes[ScheduledActionsData]

  implicit val teamTimeZoneDataReads = Json.reads[TeamTimeZoneData]
  implicit val teamTimeZoneDataWrites = Json.writes[TeamTimeZoneData]

  implicit val cityInfoDataWrites = Json.writes[CityInfo]
}
