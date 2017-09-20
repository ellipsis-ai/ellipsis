package json

case class BehaviorEditorEditConfig(
  containerId: String,
  csrfToken: Option[String],
  group: BehaviorGroupData,
  selectedId: Option[String],
  builtinParamTypes: Seq[BehaviorParameterTypeData],
  envVariables: Seq[EnvironmentVariableData],
  savedAnswers: Seq[InputSavedAnswerData],
  oauth2Applications: Seq[OAuth2ApplicationData],
  oauth2Apis: Seq[OAuth2ApiData],
  simpleTokenApis: Seq[SimpleTokenApiData],
  linkedOAuth2ApplicationIds: Seq[String],
  userId: String
)

object BehaviorEditorEditConfig {
  def fromEditorData(containerId: String, csrfToken: Option[String], data: BehaviorEditorData): BehaviorEditorEditConfig = {
    BehaviorEditorEditConfig(
      containerId,
      csrfToken,
      data.group,
      data.maybeSelectedId,
      data.builtinParamTypes,
      data.environmentVariables,
      data.savedAnswers,
      data.oauth2Applications,
      data.oauth2Apis,
      data.simpleTokenApis,
      data.linkedOAuth2ApplicationIds,
      data.userId
    )
  }
}
