package json

import java.time.OffsetDateTime

case class BehaviorEditorEditConfig(
  containerId: String,
  csrfToken: Option[String],
  group: BehaviorGroupData,
  selectedId: Option[String],
  builtinParamTypes: Seq[BehaviorParameterTypeData],
  envVariables: Seq[EnvironmentVariableData],
  savedAnswers: Seq[InputSavedAnswerData],
  awsConfigs: Seq[AWSConfigData],
  oauth2Applications: Seq[OAuth2ApplicationData],
  oauth2Apis: Seq[OAuth2ApiData],
  simpleTokenApis: Seq[SimpleTokenApiData],
  linkedOAuth2ApplicationIds: Seq[String],
  userId: String,
  isAdmin: Boolean,
  isLinkedToGithub: Boolean,
  showVersions: Option[Boolean],
  lastDeployTimestamp: Option[OffsetDateTime],
  slackTeamId: Option[String],
  botName: String
)

object BehaviorEditorEditConfig {
  def fromEditorData(containerId: String, csrfToken: Option[String], data: BehaviorEditorData, maybeShowVersions: Option[Boolean]): BehaviorEditorEditConfig = {
    BehaviorEditorEditConfig(
      containerId,
      csrfToken,
      data.group,
      data.maybeSelectedId,
      data.builtinParamTypes,
      data.environmentVariables,
      data.savedAnswers,
      data.awsConfigs,
      data.oauth2Applications,
      data.oauth2Apis,
      data.simpleTokenApis,
      data.linkedOAuth2ApplicationIds,
      data.userId,
      data.isAdmin,
      data.isLinkedToGithub,
      maybeShowVersions,
      data.lastDeployTimestamp,
      data.maybeSlackTeamId,
      data.botName
    )
  }
}
