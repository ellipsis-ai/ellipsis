package models.behaviors.behaviorparameter

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{OffsetDateTime, ZoneId}
import java.util.{Date, Locale, TimeZone}

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.joestelmach.natty.Parser
import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric
import models.behaviors._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.Normal
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.{NewParentConversation, ParentConversation}
import models.behaviors.datatypeconfig.DataTypeConfig
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.events.{Event, MessageAttachmentGroup}
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events.slack._
import models.team.Team
import play.api.libs.json._
import services.AWSLambdaConstants._
import services.caching.DataTypeBotResultsCacheKey
import services.{AWSLambdaConstants, DataService}
import slick.dbio.DBIO
import utils.Color

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class FetchValidValuesBadResultException(result: BotResult) extends Exception(s"Couldn't fetch data type values: ${result.resultType}")

sealed trait BehaviorParameterType extends FieldTypeForSchema {

  val id: String
  val exportId: String
  val name: String
  def needsConfig(dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean]
  val isBuiltIn: Boolean

  val mayRequireTypedAnswer: Boolean = false

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean]

  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[JsValue]

  def invalidPromptModifier: String

  def stopText: String = "`...stop`"

  def stopInstructions: String = s"Or say $stopText to end the conversation."

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[String]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      invalidPromptModifier + "\n\n"
    } else {
      ""
    }
  }

  def questionTextFor(context: BehaviorParameterContext, paramCount: Int, maybeRoot: Option[ParentConversation]): String = {
    val maybeRootPart = maybeRoot.map { root =>
      root.param.question
    }
    val localPart = context.parameter.question.trim
    maybeRootPart.map { rootPart =>
      s"__${rootPart}__ $localPart".trim
    }.getOrElse(s"__${localPart}__")
  }

  def preambleFor(paramCount: Int): String = {
    (if (paramCount == 2) {
      s"I need to ask you a couple of questions."
    } else if (paramCount >= 3 && paramCount < 5) {
      s"I need to ask you a few questions."
    } else if (paramCount >= 5) {
      s"I need to ask you some questions."
    } else {
      ""
    }) + "\n\n"
  }

  def promptTextForAction(
                             maybePreviousCollectedValue: Option[String],
                             context: BehaviorParameterContext,
                             maybeParamState: Option[ParamCollectionState],
                             isReminding: Boolean
                           )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[String] = {
    for {
      isFirst <- context.isFirstParamAction
      paramCount <- maybeParamState.map { paramState =>
        context.unfilledParamCountAction(paramState)
      }.getOrElse(DBIO.successful(0))
      maybeParent <- context.dataService.parentConversations.maybeForAction(context.maybeConversation)
      maybeRoot <- maybeParent.map { parent =>
        context.dataService.parentConversations.rootForParentAction(parent).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      val prefix = context.event.messageRecipientPrefix
      val invalidModifier = invalidValueModifierFor(maybePreviousCollectedValue)
      val preamble = if (isReminding || !isFirst || paramCount == 0 || invalidModifier.nonEmpty) {
        ""
      } else {
        preambleFor(paramCount)
      }
      s"""$prefix$preamble$invalidModifier ${questionTextFor(context, paramCount, maybeRoot)}"""
    }
  }

  def promptResultForAction(
                 maybePreviousCollectedValue: Option[String],
                 context: BehaviorParameterContext,
                 paramState: ParamCollectionState,
                 isReminding: Boolean
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    promptTextForAction(maybePreviousCollectedValue, context, Some(paramState), isReminding).map(context.simpleTextResultFor)
  }

  def promptResultWithValidValuesResult(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    DBIO.successful(result)
  }

  def resolvedValueForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]]

  def potentialValueFor(event: Event, context: BehaviorParameterContext): String = event.relevantMessageText

  def handleCollectedAction(event: Event, paramState: ParamCollectionState, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Unit] = {
    val potentialValue = potentialValueFor(event, context)
    val input = context.parameter.input
    if (input.isSaved) {
      resolvedValueForAction(potentialValue, context).flatMap { maybeValueToSave =>
        maybeValueToSave.map { valueToSave =>
          event.ensureUserAction(context.dataService).flatMap { user =>
            context.dataService.savedAnswers.ensureForAction(input, valueToSave, user).map(_ => {})
          }
        }.getOrElse(DBIO.successful({}))
      }
    } else {
      context.maybeConversation.map { conversation =>
        context.dataService.collectedParameterValues.ensureForAction(context.parameter, conversation, potentialValue).map(_ => {})
      }.getOrElse(DBIO.successful({}))
    }
  }

  def decorationCodeFor(param: BehaviorParameter): String = ""

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
  lazy val exportId = name
  val isBuiltIn: Boolean = true
  def needsConfig(dataService: DataService)(implicit ec: ExecutionContext) = Future.successful(false)
  def resolvedValueForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]] = {
    DBIO.successful(Some(text))
  }
  def prepareValue(text: String, team: Team): JsValue
  def prepareJsValue(value: JsValue, team: Team): JsValue
  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = DBIO.successful(prepareValue(text, context.behaviorVersion.team))
}

object TextType extends BuiltInType {
  val name = "Text"

  val outputName: String = "String"

  override val mayRequireTypedAnswer: Boolean = true

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = DBIO.successful(true)

  def prepareValue(text: String, team: Team) = JsString(text)
  def prepareJsValue(value: JsValue, team: Team): JsValue = {
    value match {
      case s: JsString => s
      case JsNull => JsNull
      case v => JsString(v.toString)
    }
  }

  val invalidPromptModifier: String = s"I need a valid answer. $stopInstructions"

}

object NumberType extends BuiltInType {
  val name = "Number"

  val outputName: String = "Float"

  override val mayRequireTypedAnswer: Boolean = true

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = DBIO.successful {
    try {
      text.toDouble
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareValue(text: String, team: Team): JsValue = try {
    JsNumber(BigDecimal(text))
  } catch {
    case e: NumberFormatException => JsString(text)
  }

  def prepareJsValue(value: JsValue, team: Team): JsValue = {
    value match {
      case n: JsNumber => n
      case s: JsString => prepareValue(s.value, team)
      case v => v
    }
  }

  val invalidPromptModifier: String = s"I need a number to answer this. $stopInstructions"
}

object DateTimeType extends BuiltInType {
  val name: String = "Date & Time"

  val outputName: String = "String"

  override val mayRequireTypedAnswer: Boolean = true

  override def questionTextFor(context: BehaviorParameterContext, paramCount: Int, maybeRoot: Option[ParentConversation]): String = {
    val tz = context.behaviorVersion.team.timeZone.getDisplayName(TextStyle.SHORT, Locale.getDefault(Locale.Category.DISPLAY))
    super.questionTextFor(context, paramCount, maybeRoot) ++ s""" (using $tz)"""
  }

  private def maybeDateFrom(text: String, defaultTimeZone: ZoneId): Option[Date] = {
    val parser = new Parser(TimeZone.getTimeZone(defaultTimeZone))
    val groups = parser.parse(text)
    if (groups.isEmpty || groups.get(0).getDates.isEmpty) {
      None
    } else {
      Some(groups.get(0).getDates.get(0))
    }
  }

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = DBIO.successful {
    try {
      maybeDateFrom(text, context.behaviorVersion.team.timeZone).isDefined
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareValue(text: String, team: Team): JsValue = {
    maybeDateFrom(text, team.timeZone).map { date =>
      JsString(OffsetDateTime.ofInstant(date.toInstant, team.timeZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }.getOrElse(JsString(text))
  }

  def prepareJsValue(value: JsValue, team: Team): JsValue = {
    value match {
      case s: JsString => prepareValue(s.value, team)
      case v => v
    }
  }

  override def decorationCodeFor(param: BehaviorParameter): String = {
    val paramName = param.input.name;
    raw"""if (!isNaN(Date.parse($paramName))) { $paramName = new Date(Date.parse($paramName)); }"""
  }

  val invalidPromptModifier: String = s"I need something I can interpret as a date & time to answer this. $stopInstructions"

}

object YesNoType extends BuiltInType {
  val name = "Yes/No"
  val yesStrings = Seq("y", "yes", "yep", "yeah", "t", "true", "sure", "why not")
  val noStrings = Seq("n", "no", "nope", "nah", "f", "false", "no way", "no chance")

  val outputName: String = "Boolean"

  def matchStringFor(text: String): String = text.toLowerCase.trim

  def maybeValidValueFor(text: String): Option[Boolean] = {
    val matchString = text.toLowerCase.trim
    if (yesStrings.contains(matchString)) {
      Some(true)
    } else if (noStrings.contains(matchString)) {
      Some(false)
    } else {
      None
    }
  }

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    DBIO.successful(maybeValidValueFor(text).isDefined)
  }

  def prepareValue(text: String, team: Team) = {
    maybeValidValueFor(text).map { vv =>
      JsBoolean(vv)
    }.getOrElse(JsString(text))
  }

  def prepareJsValue(value: JsValue, team: Team): JsValue = {
    value match {
      case b: JsBoolean => b
      case s: JsString => prepareValue(s.value, team)
      case v => v
    }
  }

  val invalidPromptModifier: String = s"I need an answer like “yes” or “no”. $stopInstructions"

  override def promptResultForAction(
                                     maybePreviousCollectedValue: Option[String],
                                     context: BehaviorParameterContext,
                                     paramState: ParamCollectionState,
                                     isReminding: Boolean
                                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    super.promptResultForAction(maybePreviousCollectedValue, context, paramState, isReminding).map { superPromptResult =>
      val callbackId = context.yesNoCallbackId
      val actionList = Seq(SlackMessageActionButton(callbackId, "Yes", YES), SlackMessageActionButton(callbackId, "No", NO))
      val actionsGroup = SlackMessageActionsGroup(callbackId, actionList, None, None, None)
      TextWithAttachmentsResult(
        superPromptResult.event,
        superPromptResult.maybeConversation,
        superPromptResult.fullText,
        superPromptResult.responseType,
        Seq(actionsGroup)
      )
    }
  }
}

object FileType extends BuiltInType {
  val name = "File"

  val outputName: String = "File"

  override val mayRequireTypedAnswer: Boolean = true

  def isIntentionallyEmpty(text: String): Boolean = text.trim.toLowerCase == "none"

  override def questionTextFor(context: BehaviorParameterContext, paramCount: Int, maybeRoot: Option[ParentConversation]): String = {
    super.questionTextFor(context, paramCount, maybeRoot) ++ """ (or type "none" if you don't have one)"""
  }

  private def eventHasFile(context: BehaviorParameterContext): Boolean = {
    context.event match {
      case e: SlackMessageEvent => e.maybeFile.nonEmpty
      case _ => false
    }
  }

  private def alreadyHasFile(text: String, context: BehaviorParameterContext): Boolean = {
    context.services.slackFileMap.maybeUrlFor(text).nonEmpty
  }

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    DBIO.successful {
      alreadyHasFile(text, context) || eventHasFile(context) || isIntentionallyEmpty(text)
    }
  }

  def prepareValue(text: String, team: Team) = {
    if (isIntentionallyEmpty(text)) {
      JsNull
    } else {
      Json.toJson(Map("id" -> text))
    }
  }

  def prepareJsValue(value: JsValue, team: Team): JsValue = {
    value match {
      case s: JsString => prepareValue(s.value, team)
      case v => v
    }
  }

  override def potentialValueFor(event: Event, context: BehaviorParameterContext): String = {
    event match {
      case e: SlackMessageEvent => e.maybeFile.map { file =>
        context.services.slackFileMap.save(file)
      }.getOrElse(super.potentialValueFor(event, context))
      case _ => super.potentialValueFor(event, context)
    }
  }

  override def decorationCodeFor(param: BehaviorParameter): String = {
    val paramName = param.input.name;
    raw"""if ($paramName) { $paramName.fetch = require("$FETCH_FUNCTION_FOR_FILE_PARAM_NAME")($paramName, $CONTEXT_PARAM); }"""
  }

  val invalidPromptModifier: String = raw"""I need you to upload a file or type `none` if you don't have one. $stopInstructions"""
}

case class ValidValue(id: String, label: String, data: JsObject)

case class BehaviorBackedDataType(dataTypeConfig: DataTypeConfig) extends BehaviorParameterType {

  val behaviorVersion = dataTypeConfig.behaviorVersion

  val id = behaviorVersion.id
  override val exportId: String = behaviorVersion.behavior.maybeExportId.getOrElse(id)
  val name = behaviorVersion.maybeName.getOrElse("Unnamed data type")

  val outputName: String = behaviorVersion.outputName
  override lazy val inputName: String = behaviorVersion.inputName

  val isBuiltIn: Boolean = false

  implicit val validValueReads = new Reads[ValidValue] {
    def reads(json: JsValue) = {
      val idProperty = json \ BehaviorParameterType.ID_PROPERTY
      for {
        id <- idProperty.validate[String].orElse(idProperty.validate[Long].map(_.toString))
        label <- (json \ BehaviorParameterType.LABEL_PROPERTY).validate[String]
      } yield {
        val otherData = json match {
          case obj: JsObject => {
            obj - BehaviorParameterType.ID_PROPERTY - BehaviorParameterType.LABEL_PROPERTY
          }
          case _ => Json.obj()
        }
        ValidValue(id, label, otherData)
      }
    }

  }
  implicit val validValueWrites = new Writes[ValidValue] {
    def writes(vv: ValidValue) = JsObject(
      Map(
        BehaviorParameterType.ID_PROPERTY -> JsString(vv.id),
        BehaviorParameterType.LABEL_PROPERTY -> JsString(vv.label)
      )
    ) ++ vv.data
  }

  override val mayRequireTypedAnswer: Boolean = true

  def resolvedValueForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]] = {
    cachedValidValueFor(text, context).map { vv =>
      DBIO.successful(Some(vv))
    }.getOrElse {
      getMatchForAction(text, context)
    }.map { maybeValidValue =>
      maybeValidValue.map(v => Json.toJson(v).toString)
    }
  }

  def editLinkFor(context: BehaviorParameterContext) = {
    val behavior = behaviorVersion.behavior
    val link = context.dataService.behaviors.editLinkFor(behavior.group.id, Some(behavior.id), context.services.configuration)
    s"[${context.parameter.paramType.name}]($link)"
  }

  def needsConfig(dataService: DataService)(implicit ec: ExecutionContext) = {
    for {
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion.groupVersion)
    } yield !requiredOAuth2ApiConfigs.forall(_.isReady)
  }

  val team = behaviorVersion.team

  def maybeValidValueForSavedAnswerAction(value: ValidValue, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[ValidValue]] = {
    getMatchForAction(value.id, context)
  }

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    maybeValidValuesForAction(text, context).map(vv => isSingleMatch(text, vv))
  }

  def maybeValidValueForText(text: String): Option[ValidValue] = {
    val maybeJson = try {
      Some(Json.parse(text))
    } catch {
      case e: JsonParseException => None
      case e: JsonMappingException => None
    }
    maybeJson.flatMap { json =>
      extractValidValueFrom(json)
    }
  }

  def maybeValidValuesForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    maybeValidValueForText(text).map { validValue =>
      maybeValidValueForSavedAnswerAction(validValue, context).map(_.toSeq)
    }.getOrElse {
      if (isCollectingOther(context)) {
        DBIO.successful(Seq(ValidValue(BehaviorParameterType.otherId, text, Json.obj())))
      } else {
        cachedValidValueFor(text, context).map { v =>
          DBIO.successful(Seq(v))
        }.getOrElse {
          getValidValuesAction(Some(text), context)
        }
      }
    }
  }

  private def cachedValuesFor(context: BehaviorParameterContext): Option[Seq[ValidValue]] = {
    for {
      conversation <- context.maybeConversation
      values <- context.cacheService.getValidValues(valuesListCacheKeyFor(conversation, context.parameter))
    } yield values
  }

  private def cachedValidValueAtIndex(text: String, context: BehaviorParameterContext): Option[ValidValue] = {
    for {
      values <- cachedValuesFor(context)
      value <- try {
        val index = text.toInt - 1
        Some(values(index))
      } catch {
        case e: NumberFormatException => None
        case e: IndexOutOfBoundsException => None
      }
    } yield value
  }

  private def cachedValidValueForLabel(text: String, context: BehaviorParameterContext): Option[ValidValue] = {
    for {
      values <- cachedValuesFor(context)
      value <- values.find((ea) => textMatchesLabel(text, ea.label, context))
    } yield value
  }

  private def cachedValidValueFor(text: String, context: BehaviorParameterContext): Option[ValidValue] = {
    cachedValidValueAtIndex(text, context).
      orElse(cachedValidValueForLabel(text, context))
  }

  def prepareForInvocation(
                            text: String,
                            context: BehaviorParameterContext
                          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[JsValue] = {
    maybeValidValuesForAction(text, context).map { validValues =>
      maybeMatchFor(text, validValues).map { vv =>
        JsObject(Map(BehaviorParameterType.ID_PROPERTY -> JsString(vv.id), BehaviorParameterType.LABEL_PROPERTY -> JsString(vv.label))) ++ vv.data
      }.getOrElse(JsString(text))
    }
  }

  val invalidPromptModifier: String = s"I need a $name. Choose one of the options below. $stopInstructions"

  private def valuesListCacheKeyFor(conversation: Conversation, parameter: BehaviorParameter): String = {
    s"values-list-${conversation.id}-${parameter.id}"
  }

  private def maybeCollectingOtherCacheKeyFor(context: BehaviorParameterContext): Option[String] = {
    context.maybeConversation.map { convo =>
      s"collecting-other-${convo.id}-${context.parameter.id}"
    }
  }

  private def paramValuesFor(maybeMatchText: Option[String], context: BehaviorParameterContext)(implicit ec: ExecutionContext): DBIO[Map[String, String]] = {
    context.dataService.behaviorParameters.allForAction(behaviorVersion).map { params =>
      if (params.isEmpty) {
        Map()
      } else {
        maybeMatchText.map { v =>
          Map(AWSLambdaConstants.invocationParamFor(0) -> v)
        }.getOrElse(Map())
      }
    }
  }

  private def fetchValidValuesResultAction(
                                            maybeMatchText: Option[String],
                                            context: BehaviorParameterContext
                                          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      paramValues <- paramValuesFor(maybeMatchText, context)
      behaviorResponse <- context.dataService.behaviorResponses.buildForAction(
        context.event,
        behaviorVersion,
        paramValues,
        None,
        None,
        context.maybeConversation.map(c => NewParentConversation(c, context.parameter)),
        userExpectsResponse = true
      )
      result <- DBIO.from(behaviorResponse.result)
    } yield result
  }

  private def dataTypeResultCacheKeyFor(maybeMatchText: Option[String], context: BehaviorParameterContext)(implicit ec: ExecutionContext): DataTypeBotResultsCacheKey = {
    DataTypeBotResultsCacheKey(context.parameter.id, maybeMatchText, context.maybeConversation.map(_.id))
  }

  private def getValidValuesResultAction(
                                          maybeMatchText: Option[String],
                                          context: BehaviorParameterContext
                                        )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      result <- DBIO.from(context.cacheService.getDataTypeBotResult(dataTypeResultCacheKeyFor(maybeMatchText, context), (key: DataTypeBotResultsCacheKey) => {
        context.dataService.run(fetchValidValuesResultAction(maybeMatchText, context))
      }))
    } yield result
  }

  private def getValidValuesSuccessResultAction(
                                                maybeMatchText: Option[String],
                                                context: BehaviorParameterContext
                                              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[SuccessResult] = {
    getValidValuesResultAction(maybeMatchText, context).map { res =>
      res match {
        case r: SuccessResult => r
        case r: BotResult => throw FetchValidValuesBadResultException(r)
      }
    }
  }

  private def extractValidValueFrom(json: JsValue): Option[ValidValue] = {
    json.validate[ValidValue] match {
      case JsSuccess(data, _) => Some(data)
      case _: JsError => None
    }
  }

  private def sendLogMessageInBackgroundFor(
                                             result: SuccessResult,
                                             context: BehaviorParameterContext
                                           )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    context.event.sendMessage(
      "",
      responseType = Normal,
      maybeShouldUnfurl = None,
      context.maybeConversation,
      Seq(),
      result.files,
      Seq(),
      result.developerContext,
      context.services
    ).map(_ => {})
  }

  private def extractValidValues(
                                  result: SuccessResult,
                                  context: BehaviorParameterContext
                                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Seq[ValidValue] = {
    result.maybeLogResult.foreach { logResult =>
      if (result.shouldIncludeLogs && logResult.authorDefinedLogStatements.nonEmpty) {
        sendLogMessageInBackgroundFor(result, context)
      }
    }
    result.result.validate[Seq[JsObject]] match {
      case JsSuccess(data, _) => {
        data.flatMap { ea =>
          extractValidValueFrom(ea)
        }
      }
      case _: JsError => {
        result.result.validate[Seq[String]] match {
          case JsSuccess(strings, _) => strings.map { ea => ValidValue(ea, ea, Json.obj()) }
          case _: JsError => Seq()
        }
      }
    }
  }

  private def validValuesFromDefaultStorageFor(
                                                maybeMatchText: Option[String],
                                                context: BehaviorParameterContext
                                              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    for {
      maybeLabelField <- context.dataService.dataTypeFields.allForAction(dataTypeConfig).map{ fields =>
        fields.find(_.isLabel).orElse {
          fields.find { ea =>
            !ea.isId && ea.fieldType == TextType
          }
        }
      }
      items <- (for {
        matchText <- maybeMatchText
        labelField <- maybeLabelField
      } yield {
        context.services.dataService.defaultStorageItems.searchByFieldAction(s"%$matchText%", labelField)
      }).getOrElse {
        context.services.dataService.defaultStorageItems.allForAction(behaviorVersion.behavior)
      }
    } yield {
      items.map(_.data).flatMap {
        case ea: JsObject =>
          val maybeLabel = maybeLabelField.flatMap { labelField => (ea \ labelField.name).asOpt[String] }
          extractValidValueFrom(Json.toJson(Map(
            "id" -> JsString(id),
            "label" -> maybeLabel.map(JsString.apply).getOrElse(JsNull)
          ) ++ ea.value))
        case _ => None
      }
    }
  }

  private def getValidValuesAction(maybeMatchText: Option[String], context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    if (dataTypeConfig.usesCode) {
      getValidValuesSuccessResultAction(maybeMatchText, context).map(res => extractValidValues(res, context))
    } else {
      validValuesFromDefaultStorageFor(maybeMatchText, context)
    }
  }

  private def getValidValues(maybeMatchText: Option[String], context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[ValidValue]] = {
    context.dataService.run(getValidValuesAction(maybeMatchText, context))
  }

  private def textMatchesLabel(text: String, label: String, context: BehaviorParameterContext): Boolean = {
    text.toLowerCase == label.toLowerCase
  }

  private def maybeMatchById(text: String, validValues: Seq[ValidValue]): Option[ValidValue] = {
    validValues.find(_.id == text)
  }

  private def maybeMatchByLabel(text: String, validValues: Seq[ValidValue]): Option[ValidValue] = {
    if (validValues.length == 1 && RatcliffObershelpMetric.compare(validValues.head.label.toLowerCase, text.toLowerCase).exists(_ > 0.75)) {
      validValues.headOption
    } else {
      None
    }
  }

  private def maybeMatchFor(text: String, validValues: Seq[ValidValue]): Option[ValidValue] = {
    maybeValidValueForText(text).orElse {
      maybeMatchById(text, validValues).orElse {
        maybeMatchByLabel(text, validValues)
      }
    }
  }

  private def isSingleMatch(text: String, validValues: Seq[ValidValue]): Boolean = {
    maybeMatchFor(text, validValues).isDefined
  }

  private def getMatchForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[ValidValue]] = {
    getValidValuesAction(Some(text), context).map { validValues =>
      maybeMatchFor(text, validValues)
    }
  }

  private def cancelAndRespondForAction(response: String, context: BehaviorParameterContext)(implicit ec: ExecutionContext): DBIO[BotResult] = {
    context.dataService.conversations.cancelAction(context.maybeConversation).map { _ =>
      context.simpleTextResultFor(s"$response\n\n(Your current action has been cancelled. Try again after fixing the problem.)")
    }
  }

  private def promptResultForOtherCaseAction(context: BehaviorParameterContext): DBIO[BotResult] = {
    DBIO.successful(context.simpleTextResultFor(s"OK, you chose “other”. What do you want to say instead?"))
  }

  private def maybeStartAgainMenuItemFor(params: Seq[BehaviorParameter]): Option[SlackMessageActionMenuItem] = {
    if (params.isEmpty) {
      None
    } else {
      // TODO: revisit this
      None //Some(SlackMessageActionMenuItem(Conversation.START_AGAIN_MENU_ITEM_TEXT, Conversation.START_AGAIN_MENU_ITEM_TEXT))
    }
  }

  private def maybeStartAgainButtonFor(params: Seq[BehaviorParameter], callbackId: String): Option[SlackMessageActionButton] = {
    if (params.isEmpty) {
      None
    } else {
      // TODO: revisit this
      None //Some(SlackMessageActionButton(callbackId, Conversation.START_AGAIN_MENU_ITEM_TEXT, Conversation.START_AGAIN_MENU_ITEM_TEXT))
    }
  }

  private def promptResultWithSimpleValidValues(
                                                 validValues: Seq[ValidValue],
                                                 context: BehaviorParameterContext,
                                                 params: Seq[BehaviorParameter]
                                               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    promptTextForAction(None, context, None, false).map { text =>
      val callbackId = context.dataTypeChoiceCallbackId
      val actionList = validValues.map { ea =>
        SlackMessageActionButton(callbackId, ea.label, ea.label)
      } ++ Seq(
        maybeStartAgainButtonFor(params, callbackId),
        Some(SlackMessageActionButton(callbackId, Conversation.CANCEL_MENU_ITEM_TEXT, Conversation.CANCEL_MENU_ITEM_TEXT))
      ).flatten
      val actionsGroup = SlackMessageActionsGroup(callbackId, actionList, None, None, None)
      TextWithAttachmentsResult(
        context.event,
        context.maybeConversation,
        text,
        context.behaviorVersion.responseType,
        Seq(actionsGroup)
      )
    }
  }

  private val MAX_SIMPLE_BUTTON_LABEL_LENGTH = 20
  private val MAX_SIMPLE_BUTTONS = 4

  private def isSimpleValidValue(validValue: ValidValue): Boolean = {
    val withoutEmoji = validValue.label.replaceAll(":[a-z_]+:", "")
    withoutEmoji.length <= MAX_SIMPLE_BUTTON_LABEL_LENGTH
  }

  private def areValidValuesSimple(validValues: Seq[ValidValue], params: Seq[BehaviorParameter]): Boolean = {
    val buttonLength = if (params.isEmpty) { validValues.length } else { validValues.length - 1 }
    buttonLength <= MAX_SIMPLE_BUTTONS && validValues.forall(isSimpleValidValue)
  }

  private def promptResultWithValidValues(validValues: Seq[ValidValue], context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      params <- context.services.dataService.behaviorParameters.allForAction(behaviorVersion)
      output <- if (validValues.isEmpty) {
        if (params.isEmpty) {
          cancelAndRespondForAction(s"This data type isn't returning any values: ${editLinkFor(context)}", context)
        } else {
          context.maybeConversation.map { convo =>
            context.dataService.collectedParameterValues.deleteForAction(context.parameter, convo).flatMap { _ =>
              convo.respondAction(context.event, isReminding = false, context.services)
            }
          }.getOrElse(cancelAndRespondForAction(s"This data type isn't returning any values: ${editLinkFor(context)}", context))
        }
      } else {
        context.maybeConversation.foreach { conversation =>
          context.cacheService.cacheValidValues(valuesListCacheKeyFor(conversation, context.parameter), validValues)
        }
        if (areValidValuesSimple(validValues, params)) {
          promptResultWithSimpleValidValues(validValues, context, params)
        } else {
          val builtinMenuItems = Seq(
            maybeStartAgainMenuItemFor(params),
            Some(SlackMessageActionMenuItem(Conversation.CANCEL_MENU_ITEM_TEXT, Conversation.CANCEL_MENU_ITEM_TEXT))
          ).flatten
          val menuItems = validValues.zipWithIndex.map { case (ea, i) =>
            SlackMessageActionMenuItem(s"${i+1}. ${ea.label}", ea.label)
          } ++ builtinMenuItems
          val actionsList = Seq(SlackMessageActionMenu("ignored", "Choose an option", menuItems))
          val groups: Seq[MessageAttachmentGroup] = Seq(
            SlackMessageActionsGroup(context.dataTypeChoiceCallbackId, actionsList, None, None, Some(Color.BLUE_LIGHT))
          )
          promptTextForAction(None, context, None, false).map { text =>
            TextWithAttachmentsResult(context.event, context.maybeConversation, text, context.behaviorVersion.responseType, groups)
          }
        }
      }
    } yield output
  }

  override def promptResultWithValidValuesResult(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    val validValues = result match {
      case r: SuccessResult => extractValidValues(r, context)
      case _ => Seq()
    }
    promptResultWithValidValues(validValues, context)
  }

  private def isCollectingOther(context: BehaviorParameterContext): Boolean = {
    maybeCollectingOtherCacheKeyFor(context).exists { key =>
      context.cacheService.hasKey(key)
    }
  }

  override def promptResultForAction(
                               maybePreviousCollectedValue: Option[String],
                               context: BehaviorParameterContext,
                               paramState: ParamCollectionState,
                               isReminding: Boolean
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    if (dataTypeConfig.usesCode) {
      if (isCollectingOther(context)) {
        promptResultForOtherCaseAction(context)
      } else {
        for {
          initialResult <- getValidValuesResultAction(maybePreviousCollectedValue, context)
          result <- if (initialResult.maybeConversation.isDefined) {
            DBIO.successful(initialResult)
          } else {
            promptResultWithValidValuesResult(initialResult, context)
          }
        } yield result
      }
    } else {
      for {
        validValues <- validValuesFromDefaultStorageFor(None, context)
        result <- promptResultWithValidValues(validValues, context)
      } yield result
    }
  }

  private def clearCollectedValuesForAction(context: BehaviorParameterContext)(implicit ec: ExecutionContext): DBIO[Unit] = {
    context.maybeConversation.map { convo =>
      for {
        collected <- context.dataService.collectedParameterValues.allForAction(convo)
        _ <- DBIO.sequence(collected.map { ea =>
          context.dataService.collectedParameterValues.deleteForAction(ea.parameter, convo)
        })
      } yield {}
    }.getOrElse(DBIO.successful({}))
  }

  override def handleCollectedAction(
                                      event: Event,
                                      paramState: ParamCollectionState,
                                      context: BehaviorParameterContext
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Unit] = {
    if (!isCollectingOther(context) && isOther(context) && context.maybeConversation.isDefined) {
      maybeCollectingOtherCacheKeyFor(context).foreach { key =>
        context.cacheService.set(key, "true", 5.minutes)
      }
      DBIO.successful({})
    } else if (isRequestingStartAgain(context)) {
      clearCollectedValuesForAction(context)
    } else {
      super.handleCollectedAction(event, paramState, context)
    }
  }

  def isOther(context: BehaviorParameterContext): Boolean = {
    cachedValidValueFor(context.event.relevantMessageText, context).exists { v =>
      v.id.toLowerCase == BehaviorParameterType.otherId
    }
  }

  def isRequestingStartAgain(context: BehaviorParameterContext): Boolean = {
    context.event.relevantMessageText == Conversation.START_AGAIN_MENU_ITEM_TEXT
  }

}

object BehaviorParameterType {

  val ID_PROPERTY = "id"
  val LABEL_PROPERTY = "label"
  val DATA_PROPERTY = "data"
  val SEARCH_COUNT_THRESHOLD = 30
  val otherId: String = "other"

  val allBuiltin = Seq(
    TextType,
    NumberType,
    YesNoType,
    DateTimeType,
    FileType
  )

  def findBuiltIn(id: String): Option[BehaviorParameterType] = allBuiltin.find(_.id == id)

  def allForAction(maybeBehaviorGroupVersion: Option[BehaviorGroupVersion], dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[BehaviorParameterType]] = {
    maybeBehaviorGroupVersion.map { groupVersion =>
      dataService.dataTypeConfigs.allForAction(groupVersion)
    }.getOrElse(DBIO.successful(Seq())).map { dataTypeConfigs =>
      allBuiltin ++ dataTypeConfigs.map(BehaviorBackedDataType.apply)
    }
  }

  def findAction(id: String, behaviorGroupVersion: BehaviorGroupVersion, dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[BehaviorParameterType]] = {
    allForAction(Some(behaviorGroupVersion), dataService).map { all =>
      all.find {
        case paramType: BehaviorBackedDataType => paramType.id == id || paramType.behaviorVersion.maybeExportId.contains(id)
        case paramType: BehaviorParameterType => paramType.id == id
      }
    }
  }

}
