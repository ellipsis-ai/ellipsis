package json

import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.{ExecutionErrorData, ExecutionLogData}
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.SlackMessage
import models.behaviors.testing.{InvocationTestReportOutput, ResultOutput}
import play.api.libs.json._
import services.SlackMessageEventData
import utils.{CityInfo, UploadFileSpec}

object Formatting {

  implicit val behaviorParameterTypeReads = Json.reads[BehaviorParameterTypeData]
  implicit val behaviorParameterTypeWrites = Json.writes[BehaviorParameterTypeData]

  implicit val inputSavedAnswerReads = Json.reads[InputSavedAnswerData]
  implicit val inputSavedAnswerWrites = Json.writes[InputSavedAnswerData]

  implicit val inputReads = Json.reads[InputData]
  implicit val inputWrites = Json.writes[InputData]

  implicit val dataTypeFieldReads = Json.reads[DataTypeFieldData]
  implicit val dataTypeFieldWrites = Json.writes[DataTypeFieldData]

  implicit val dataTypeConfigReads = Json.reads[DataTypeConfigData]
  implicit val dataTypeConfigWrites = Json.writes[DataTypeConfigData]

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

  implicit val nodeModuleVersionDataReads = Json.reads[NodeModuleVersionData]
  implicit val nodeModuleVersionDataWrites = Json.writes[NodeModuleVersionData]

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

  implicit val behaviorEditorEditConfigWrites = Json.writes[BehaviorEditorEditConfig]

  implicit val environmentVariablesListConfigWrites = Json.writes[EnvironmentVariablesListConfig]

  implicit val applicationIndexConfig = Json.writes[ApplicationIndexConfig]

  implicit val apiTokenReads = Json.reads[APITokenData]
  implicit val apiTokenWrites = Json.writes[APITokenData]

  implicit val apiTokenListConfigWrites = Json.writes[APITokenListConfig]

  implicit val invocationLogEntryReads = Json.reads[InvocationLogEntryData]
  implicit val invocationLogEntryWrites = Json.writes[InvocationLogEntryData]

  implicit val invocationLogEntriesByDayReads = Json.reads[InvocationLogsByDayData]
  implicit val invocationLogEntriesByDayWrites = Json.writes[InvocationLogsByDayData]

  implicit val scheduledActionArgumentDataReads = Json.reads[ScheduledActionArgumentData]
  implicit val scheduledActionArgumentDataWrites = Json.writes[ScheduledActionArgumentData]

  implicit val ScheduledActionRecurrenceTimeDataReads = Json.reads[ScheduledActionRecurrenceTimeData]
  implicit val ScheduledActionRecurrenceTimeDataWrites = Json.writes[ScheduledActionRecurrenceTimeData]

  implicit val scheduledActionRecurrenceDataReads = Json.reads[ScheduledActionRecurrenceData]
  implicit val scheduledActionRecurrenceDataWrites = Json.writes[ScheduledActionRecurrenceData]

  implicit val scheduledActionDataReads = Json.reads[ScheduledActionData]
  implicit val scheduledActionDataWrites = Json.writes[ScheduledActionData]

  implicit val scheduleChannelDataReads = Json.reads[ScheduleChannelData]
  implicit val scheduleChannelDataWrites = Json.writes[ScheduleChannelData]

  implicit val scheduledActionsConfigReads = Json.reads[ScheduledActionsConfig]
  implicit val scheduledActionsConfigWrites = Json.writes[ScheduledActionsConfig]

  implicit val teamTimeZoneDataReads = Json.reads[TeamTimeZoneData]
  implicit val teamTimeZoneDataWrites = Json.writes[TeamTimeZoneData]

  implicit val cityInfoDataWrites = Json.writes[CityInfo]

  implicit val defaultStorageItemDataReads = Json.reads[DefaultStorageItemData]
  implicit val defaultStorageItemDataWrites = Json.writes[DefaultStorageItemData]

  implicit val slackBotProfileReads = Json.reads[SlackBotProfile]
  implicit val slackBotProfileWrites = Json.writes[SlackBotProfile]

  implicit val slackUserInfoReads = Json.reads[SlackUserInfo]
  implicit val slackUserInfoWrites = Json.writes[SlackUserInfo]

  implicit val slackMessageReads = Json.reads[SlackMessage]
  implicit val slackMessageWrites = Json.writes[SlackMessage]

  implicit val slackMessageEventDataReads = Json.reads[SlackMessageEventData]
  implicit val slackMessageEventDataWrites = Json.writes[SlackMessageEventData]

  implicit val validValueReads = Json.reads[ValidValue]
  implicit val validValueWrites = Json.writes[ValidValue]

  implicit val slackFileSpecReads = Json.reads[UploadFileSpec]
  implicit val slackFileSpecWrites = Json.writes[UploadFileSpec]

  implicit val resultOutputWrites = Json.writes[ResultOutput]
  implicit val testReportOutputWrites = Json.writes[InvocationTestReportOutput]

  implicit val executionLogReads = Json.reads[ExecutionLogData]
  implicit val executionErrorValueReads = Json.reads[ExecutionErrorData]

  implicit val behaviorGroupVersionMetaDataWrites = Json.writes[BehaviorGroupVersionMetaData]
}
