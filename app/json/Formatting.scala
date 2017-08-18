package json

import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.SlackMessage
import play.api.libs.json._
import services.SlackMessageEventData
import utils.{CityInfo, UploadFileSpec}

object Formatting {

  lazy implicit val behaviorParameterTypeReads = Json.reads[BehaviorParameterTypeData]
  lazy implicit val behaviorParameterTypeWrites = Json.writes[BehaviorParameterTypeData]

  lazy implicit val inputSavedAnswerReads = Json.reads[InputSavedAnswerData]
  lazy implicit val inputSavedAnswerWrites = Json.writes[InputSavedAnswerData]

  lazy implicit val inputReads = Json.reads[InputData]
  lazy implicit val inputWrites = Json.writes[InputData]

  lazy implicit val dataTypeFieldReads = Json.reads[DataTypeFieldData]
  lazy implicit val dataTypeFieldWrites = Json.writes[DataTypeFieldData]

  lazy implicit val dataTypeConfigReads = Json.reads[DataTypeConfigData]
  lazy implicit val dataTypeConfigWrites = Json.writes[DataTypeConfigData]

  lazy implicit val libraryVersionReads = Json.reads[LibraryVersionData]
  lazy implicit val libraryVersionWrites = Json.writes[LibraryVersionData]

  lazy implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  lazy implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  lazy implicit val awsConfigReads = Json.reads[AWSConfigData]
  lazy implicit val awsConfigWrites = Json.writes[AWSConfigData]

  lazy implicit val oAuth2ApiReads = Json.reads[OAuth2ApiData]
  lazy implicit val oAuth2ApiWrites = Json.writes[OAuth2ApiData]

  lazy implicit val oAuth2ApplicationReads = Json.reads[OAuth2ApplicationData]
  lazy implicit val oAuth2ApplicationWrites = Json.writes[OAuth2ApplicationData]

  lazy implicit val oAuth2ApplicationListConfigWrites = Json.writes[OAuth2ApplicationListConfig]

  lazy implicit val oAuth2ApplicationEditConfigWrites = Json.writes[OAuth2ApplicationEditConfig]

  lazy implicit val requiredOAuth2ApiConfigReads = Json.reads[RequiredOAuth2ApiConfigData]
  lazy implicit val requiredOAuth2ApiConfigWrites = Json.writes[RequiredOAuth2ApiConfigData]

  lazy implicit val simpleTokenApiReads = Json.reads[SimpleTokenApiData]
  lazy implicit val simpleTokenApiWrites = Json.writes[SimpleTokenApiData]

  lazy implicit val requiredSimpleTokenApiReads = Json.reads[RequiredSimpleTokenApiData]
  lazy implicit val requiredSimpleTokenApiWrites = Json.writes[RequiredSimpleTokenApiData]

  lazy implicit val behaviorConfigReads = Json.reads[BehaviorConfig]
  lazy implicit val behaviorConfigWrites = Json.writes[BehaviorConfig]

  lazy implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  lazy implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  lazy implicit val behaviorReads = Json.reads[BehaviorData]
  lazy implicit val behaviorWrites = Json.writes[BehaviorData]

  lazy implicit val nodeModuleVersionDataReads = Json.reads[NodeModuleVersionData]
  lazy implicit val nodeModuleVersionDataWrites = Json.writes[NodeModuleVersionData]

  lazy implicit val behaviorGroupConfigReads = Json.reads[BehaviorGroupConfig]
  lazy implicit val behaviorGroupConfigWrites = Json.writes[BehaviorGroupConfig]

  lazy implicit val behaviorGroupReads = Json.reads[BehaviorGroupData]
  lazy implicit val behaviorGroupWrites = Json.writes[BehaviorGroupData]

  lazy implicit val installedBehaviorReads = Json.reads[InstalledBehaviorGroupData]
  lazy implicit val installedBehaviorWrites = Json.writes[InstalledBehaviorGroupData]

  lazy implicit val environmentVariableReads = Json.reads[EnvironmentVariableData]
  lazy implicit val environmentVariableWrites = Json.writes[EnvironmentVariableData]

  lazy implicit val environmentVariablesReads = Json.reads[EnvironmentVariablesData]
  lazy implicit val environmentVariablesWrites = Json.writes[EnvironmentVariablesData]

  lazy implicit val behaviorEditorEditConfigWrites = Json.writes[BehaviorEditorEditConfig]

  lazy implicit val environmentVariablesListConfigWrites = Json.writes[EnvironmentVariablesListConfig]

  lazy implicit val applicationIndexConfig = Json.writes[ApplicationIndexConfig]

  lazy implicit val apiTokenReads = Json.reads[APITokenData]
  lazy implicit val apiTokenWrites = Json.writes[APITokenData]

  lazy implicit val apiTokenListConfigWrites = Json.writes[APITokenListConfig]

  lazy implicit val invocationLogEntryReads = Json.reads[InvocationLogEntryData]
  lazy implicit val invocationLogEntryWrites = Json.writes[InvocationLogEntryData]

  lazy implicit val invocationLogEntriesByDayReads = Json.reads[InvocationLogsByDayData]
  lazy implicit val invocationLogEntriesByDayWrites = Json.writes[InvocationLogsByDayData]

  lazy implicit val scheduledActionArgumentDataReads = Json.reads[ScheduledActionArgumentData]
  lazy implicit val scheduledActionArgumentDataWrites = Json.writes[ScheduledActionArgumentData]

  lazy implicit val ScheduledActionRecurrenceTimeDataReads = Json.reads[ScheduledActionRecurrenceTimeData]
  lazy implicit val ScheduledActionRecurrenceTimeDataWrites = Json.writes[ScheduledActionRecurrenceTimeData]

  lazy implicit val scheduledActionRecurrenceDataReads = Json.reads[ScheduledActionRecurrenceData]
  lazy implicit val scheduledActionRecurrenceDataWrites = Json.writes[ScheduledActionRecurrenceData]

  lazy implicit val scheduledActionDataReads = Json.reads[ScheduledActionData]
  lazy implicit val scheduledActionDataWrites = Json.writes[ScheduledActionData]

  lazy implicit val scheduleChannelDataReads = Json.reads[ScheduleChannelData]
  lazy implicit val scheduleChannelDataWrites = Json.writes[ScheduleChannelData]

  lazy implicit val scheduledActionsConfigReads = Json.reads[ScheduledActionsConfig]
  lazy implicit val scheduledActionsConfigWrites = Json.writes[ScheduledActionsConfig]

  lazy implicit val teamTimeZoneDataReads = Json.reads[TeamTimeZoneData]
  lazy implicit val teamTimeZoneDataWrites = Json.writes[TeamTimeZoneData]

  lazy implicit val cityInfoDataWrites = Json.writes[CityInfo]

  lazy implicit val defaultStorageItemDataReads = Json.reads[DefaultStorageItemData]
  lazy implicit val defaultStorageItemDataWrites = Json.writes[DefaultStorageItemData]

  lazy implicit val slackBotProfileReads = Json.reads[SlackBotProfile]
  lazy implicit val slackBotProfileWrites = Json.writes[SlackBotProfile]

  lazy implicit val slackUserInfoReads = Json.reads[SlackUserInfo]
  lazy implicit val slackUserInfoWrites = Json.writes[SlackUserInfo]

  lazy implicit val slackMessageReads = Json.reads[SlackMessage]
  lazy implicit val slackMessageWrites = Json.writes[SlackMessage]

  lazy implicit val slackMessageEventDataReads = Json.reads[SlackMessageEventData]
  lazy implicit val slackMessageEventDataWrites = Json.writes[SlackMessageEventData]

  lazy implicit val validValueReads = Json.reads[ValidValue]
  lazy implicit val validValueWrites = Json.writes[ValidValue]

  lazy implicit val slackFileSpecReads = Json.reads[UploadFileSpec]
  lazy implicit val slackFileSpecWrites = Json.writes[UploadFileSpec]

}
