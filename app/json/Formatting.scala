package json

import org.joda.time.LocalDateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._

object Formatting {

  def jodaLocalDateTimeReads(pattern: String, corrector: String => String = identity): Reads[LocalDateTime] = new Reads[LocalDateTime] {

    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

    val df = if (pattern == "") ISODateTimeFormat.localDateOptionalTimeParser else DateTimeFormat.forPattern(pattern)

    def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(s) => parseDateTime(corrector(s)) match {
        case Some(dt) => JsSuccess(dt)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("Expected joda local datetime", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("Expected joda local datetime"))))
    }

    private def parseDateTime(input: String): Option[LocalDateTime] =
      scala.util.control.Exception.allCatch[LocalDateTime] opt (LocalDateTime.parse(input, df))
  }

  def jodaLocalDateTimeWrites(pattern: String): Writes[org.joda.time.LocalDateTime] = new Writes[org.joda.time.LocalDateTime] {
    def writes(d: org.joda.time.LocalDateTime): JsValue = JsString(d.toString(pattern))
  }

  val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  implicit val localDateTimeFormat =
    Format[LocalDateTime](jodaLocalDateTimeReads(dateTimePattern), jodaLocalDateTimeWrites(dateTimePattern))

  implicit val behaviorParameterTypeReads = Json.reads[BehaviorParameterTypeData]
  implicit val behaviorParameterTypeWrites = Json.writes[BehaviorParameterTypeData]

  implicit val inputSavedAnswerReads = Json.reads[InputSavedAnswerData]
  implicit val inputSavedAnswerWrites = Json.writes[InputSavedAnswerData]

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
}
