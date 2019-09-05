package json

import ai.x.play.json.Jsonx
import json.slack.dialogs.{SlackDialogInput, SlackDialogSelectInput, SlackDialogSelectOption, SlackDialogTextInput}
import json.web.settings.IntegrationListConfig
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors._
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.behaviortestresult.BehaviorTestResult
import models.behaviors.events.slack.{SlackFile, SlackMessage}
import models.behaviors.testing.{InvocationTestReportOutput, ResultOutput}
import play.api.libs.json._
import services.AWSLambdaLogResult
import services.caching.{DeveloperContextData, ParameterWithValueData, SlackMessageEventData, SuccessResultData}
import utils._

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

  lazy implicit val legacyBehaviorTriggerJsonFormat = Json.format[LegacyBehaviorTriggerJson]

  lazy implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  lazy implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  lazy implicit val awsConfigReads = Json.reads[AWSConfigData]
  lazy implicit val awsConfigWrites = Json.writes[AWSConfigData]

  lazy implicit val oAuthApiFormat = Json.format[OAuthApiData]
  lazy implicit val oAuthApplicatioFormat = Json.format[OAuthApplicationData]

  lazy implicit val linkedGithubRepoDataFormat = Json.format[LinkedGithubRepoData]

  lazy implicit val userDataFormat = Json.format[UserData]

  lazy implicit val oAuthApplicationEditConfigFormat = Jsonx.formatCaseClass[OAuthApplicationEditConfig]

  lazy implicit val requiredOAuthApiConfigFormat = Json.format[RequiredOAuthApiConfigData]

  lazy implicit val simpleTokenApiReads = Json.reads[SimpleTokenApiData]
  lazy implicit val simpleTokenApiWrites = Json.writes[SimpleTokenApiData]

  lazy implicit val requiredSimpleTokenApiReads = Json.reads[RequiredSimpleTokenApiData]
  lazy implicit val requiredSimpleTokenApiWrites = Json.writes[RequiredSimpleTokenApiData]

  lazy implicit val behaviorResponseTypeDataFormat = Json.format[BehaviorResponseTypeData]

  lazy implicit val legacyBehaviorConfigJson = Json.format[LegacyBehaviorConfigJson]

  lazy implicit val behaviorConfigReads = Json.reads[BehaviorConfig]
  lazy implicit val behaviorConfigWrites = Json.writes[BehaviorConfig]

  lazy implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  lazy implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  lazy implicit val behaviorReads = Json.reads[BehaviorData]
  lazy implicit val behaviorWrites = Json.writes[BehaviorData]

  lazy implicit val nodeModuleVersionDataReads = Json.reads[NodeModuleVersionData]
  lazy implicit val nodeModuleVersionDataWrites = Json.writes[NodeModuleVersionData]

  lazy implicit val requiredAWSConfigDataReads = Json.reads[RequiredAWSConfigData]
  lazy implicit val requiredAWSConfigDataWrites = Json.writes[RequiredAWSConfigData]

  lazy implicit val slackUserProfileDataReads = Json.reads[SlackUserProfileData]
  lazy implicit val slackUserProfileDataWrites = Json.writes[SlackUserProfileData]

  lazy implicit val slackUserTeamIdsFormat = Json.format[SlackUserTeamIds]

  lazy implicit val slackUserDataReads = Json.reads[SlackUserData]
  lazy implicit val slackUserDataWrites = Json.writes[SlackUserData]

  lazy implicit val behaviorGroupDeploymentDataFormat = Json.format[BehaviorGroupDeploymentData]

  lazy implicit val behaviorGroupMetaDataFormat = Json.format[BehaviorGroupMetaData]

  lazy implicit val behaviorGroupReads = Json.reads[BehaviorGroupData]
  lazy implicit val behaviorGroupWrites = Json.writes[BehaviorGroupData]

  lazy implicit val immutableBehaviorGroupVersionDataFormat = Json.format[ImmutableBehaviorGroupVersionData]

  lazy implicit val behaviorGroupConfigReads = Json.reads[BehaviorGroupConfig]
  lazy implicit val behaviorGroupConfigWrites = Json.writes[BehaviorGroupConfig]

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

  lazy implicit val scheduledActionArgumentDataReads = Json.reads[ScheduledActionArgumentData]
  lazy implicit val scheduledActionArgumentDataWrites = Json.writes[ScheduledActionArgumentData]

  lazy implicit val ScheduledActionRecurrenceTimeDataReads = Json.reads[ScheduledActionRecurrenceTimeData]
  lazy implicit val ScheduledActionRecurrenceTimeDataWrites = Json.writes[ScheduledActionRecurrenceTimeData]

  lazy implicit val scheduledActionRecurrenceDataReads = Json.reads[ScheduledActionRecurrenceData]
  lazy implicit val scheduledActionRecurrenceDataWrites = Json.writes[ScheduledActionRecurrenceData]

  lazy implicit val scheduledActionNextRecurrencesData = Json.writes[ScheduledActionValidatedRecurrenceData]

  lazy implicit val scheduledActionDataReads = Json.reads[ScheduledActionData]
  lazy implicit val scheduledActionDataWrites = Json.writes[ScheduledActionData]

  lazy implicit val scheduleChannelDataReads = Json.reads[ScheduleChannelData]
  lazy implicit val scheduleChannelDataWrites = Json.writes[ScheduleChannelData]

  lazy implicit val recurrenceValidationDataFormat = Json.format[RecurrenceValidationData]
  lazy implicit val triggerValidationDataFormat = Json.format[ValidateTriggersRequestData]
  lazy implicit val validBehaviorIdTriggerDataFormat = Json.format[ValidBehaviorIdTriggerData]
  lazy implicit val validTriggerDataFormat = Json.format[ValidTriggerData]

  lazy implicit val teamChannelsDataFormat = Json.format[TeamChannelsData]
  lazy implicit val orgChannelsDataFormat = Json.format[OrgChannelsData]

  lazy implicit val scheduledActionsConfigReads = Json.reads[ScheduledActionsConfig]
  lazy implicit val scheduledActionsConfigWrites = Json.writes[ScheduledActionsConfig]

  lazy implicit val teamTimeZoneDataReads = Json.reads[TimeZoneData]
  lazy implicit val teamTimeZoneDataWrites = Json.writes[TimeZoneData]

  lazy implicit val cityInfoDataWrites = Json.writes[CityInfo]

  lazy implicit val defaultStorageItemDataReads = Json.reads[DefaultStorageItemData]
  lazy implicit val defaultStorageItemDataWrites = Json.writes[DefaultStorageItemData]

  lazy implicit val slackBotProfileReads = Json.reads[SlackBotProfile]
  lazy implicit val slackBotProfileWrites = Json.writes[SlackBotProfile]

  lazy implicit val slackMessageReads = Json.reads[SlackMessage]
  lazy implicit val slackMessageWrites = Json.writes[SlackMessage]

  lazy implicit val slackFileReads = Json.reads[SlackFile]
  lazy implicit val slackFileWrites = Json.writes[SlackFile]

  lazy implicit val slackMessageEventDataReads = Json.reads[SlackMessageEventData]
  lazy implicit val slackMessageEventDataWrites = Json.writes[SlackMessageEventData]

  lazy implicit val parameterValueFormat = Json.format[ParameterValue]
  lazy implicit val parameterWithValueDataFormat = Json.format[ParameterWithValueData]
  lazy implicit val developerContextDataFormat = Json.format[DeveloperContextData]
  lazy implicit val awsLambdaLogResultFormat = Json.format[AWSLambdaLogResult]
  lazy implicit val successResultDataFormat = Json.format[SuccessResultData]

  lazy implicit val actionArgDataFormat = Json.format[ActionArgData]
  lazy implicit val actionChoiceDataFormat = Json.format[ActionChoiceData]
  lazy implicit val invocationLogEntryReads = Json.reads[InvocationLogEntryData]
  lazy implicit val invocationLogEntryWrites = Json.writes[InvocationLogEntryData]

  lazy implicit val invocationLogEntriesByDayReads = Json.reads[InvocationLogsByDayData]
  lazy implicit val invocationLogEntriesByDayWrites = Json.writes[InvocationLogsByDayData]

  lazy implicit val validValueReads = Json.reads[ValidValue]
  lazy implicit val validValueWrites = Json.writes[ValidValue]

  lazy implicit val slackFileSpecReads = Json.reads[UploadFileSpec]
  lazy implicit val slackFileSpecWrites = Json.writes[UploadFileSpec]

  lazy implicit val resultOutputWrites = Json.writes[ResultOutput]
  lazy implicit val testReportOutputWrites = Json.writes[InvocationTestReportOutput]

  lazy implicit val awsConfigEditConfigReads = Json.reads[AWSConfigEditConfig]
  lazy implicit val awsConfigEditConfigWrites = Json.writes[AWSConfigEditConfig]

  lazy implicit val awsConfigListConfigReads = Json.reads[AWSConfigListConfig]
  lazy implicit val awsConfigListConfigWrites = Json.writes[AWSConfigListConfig]

  lazy implicit val executionLogReads = Json.reads[ExecutionLogData]
  lazy implicit val executionErrorValueReads = Json.reads[ExecutionErrorData]

  lazy implicit val behaviorGroupVersionMetaDataWrites = Json.writes[BehaviorGroupVersionMetaData]

  lazy implicit val regionalSettingsConfigFormat = Json.format[RegionalSettingsConfig]

  lazy implicit val logEntryDataFormat = Json.format[LogEntryData]

  lazy implicit val apiErrorDataFormat = Json.format[APIErrorData]
  lazy implicit val apiErrorResultDataFormat = Json.format[APIResultWithErrorsData]

  lazy implicit val linkedAccountDataFormat = Json.format[LinkedAccountData]

  lazy implicit val githubConfigConfigFormat = Json.format[GithubConfigConfig]

  lazy implicit val integrationListConfigFormat = Json.format[IntegrationListConfig]

  lazy implicit val actionArgFormat = Json.format[ActionArg]
  lazy implicit val nextActionFormat = Json.format[NextAction]
  lazy implicit val skillCodeActionChoiceFormat = Json.format[SkillCodeActionChoice]
  lazy implicit val actionChoiceFormat = Json.format[ActionChoice]

  lazy implicit val supportRequestConfigFormat = Json.format[SupportRequestConfig]

  lazy implicit val slackConversationTopicFormat: Format[SlackConversationTopic] = Json.format[SlackConversationTopic]
  lazy implicit val slackConversationPurposeFormat: Format[SlackConversationPurpose] = Json.format[SlackConversationPurpose]
  lazy implicit val slackConversationLatestInfo: Format[SlackConversationLatestInfo] = Json.format[SlackConversationLatestInfo]
  lazy implicit val slackConversationFormat: Format[SlackConversation] = Json.format[SlackConversation]

  lazy implicit val behaviorTestResultFormat: Format[BehaviorTestResult] = Json.format[BehaviorTestResult]
  lazy implicit val behaviorTestResultsDataFormat: Format[BehaviorTestResultsData] = Json.format[BehaviorTestResultsData]

  lazy implicit val adminTeamDataWrites = Json.writes[AdminTeamData]

  lazy implicit val chartDataPointFormat = Json.format[ChartDataPoint]
  lazy implicit val usageReportDataFormat = Json.format[UsageReportData]
  lazy implicit val usageReportConfigFormat = Json.format[UsageReportConfig]
  lazy implicit val skillManifestItemDataFormat = Json.format[SkillManifestItemData]
  lazy implicit val skillManifestConfigFormat = Json.format[SkillManifestConfig]

  lazy implicit val messageListenerDataFormat = Json.format[MessageListenerData]

  lazy implicit val dialogState = Json.format[DialogState]
  lazy implicit val slackDialogTextInputWrite = Json.writes[SlackDialogTextInput]
  lazy implicit val slackDialogSelectOption = Json.writes[SlackDialogSelectOption]
  lazy implicit val slackDialogSelectInputWrite = Json.writes[SlackDialogSelectInput]
  lazy implicit val slackDialogInputWrite = Json.writes[SlackDialogInput]
  lazy implicit val slackDialogParamsWrite = Json.writes[SlackDialogParams]

}

