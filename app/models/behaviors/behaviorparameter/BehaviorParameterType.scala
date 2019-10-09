package models.behaviors.behaviorparameter

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{OffsetDateTime, ZoneId}
import java.util.{Date, Locale, TimeZone}

import akka.actor.ActorSystem
import com.joestelmach.natty.Parser
import models.behaviors._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.parentconversation.ParentConversation
import models.behaviors.datatypeconfig.DataTypeConfig
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events.{Event, MessageEvent}
import models.team.Team
import play.api.libs.json._
import services.AWSLambdaConstants._
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class FetchValidValuesBadResultException(result: BotResult) extends Exception(s"Couldn't fetch data type values: ${result.resultType}")

sealed trait BehaviorParameterType extends FieldTypeForSchema {

  val id: String
  def exportId(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String]
  val name: String
  val typescriptType: String

  def needsConfigAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Boolean]
  def needsConfig(dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    dataService.run(needsConfigAction(dataService))
  }
  val isBuiltIn: Boolean

  val mayRequireTypedAnswer: Boolean = false

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean]

  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[JsValue]

  val invalidValueText: String

  def invalidPromptModifier: String = s"$invalidValueText $stopInstructions"

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

  def validValuesFor(
                      result: BotResult,
                      context: BehaviorParameterContext
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    DBIO.successful(Seq())
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
  def exportId(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful(name)
  val isBuiltIn: Boolean = true
  def needsConfigAction(dataService: DataService)(implicit ec: ExecutionContext) = DBIO.successful(false)
  def resolvedValueForAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]] = {
    DBIO.successful(Some(text))
  }
  def prepareValue(text: String, team: Team): JsValue
  def prepareJsValue(value: JsValue, team: Team): JsValue
  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = DBIO.successful(prepareValue(text, context.behaviorVersion.team))
}

trait BuiltInTextualType extends BuiltInType {

  override def promptResultForAction(
                                      maybePreviousCollectedValue: Option[String],
                                      context: BehaviorParameterContext,
                                      paramState: ParamCollectionState,
                                      isReminding: Boolean
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    super.promptResultForAction(maybePreviousCollectedValue, context, paramState, isReminding).map { superPromptResult =>
      val callbackId = context.textInputCallbackId
      val eventContext = context.event.eventContext
      eventContext.maybeMessageActionTextInputFor(callbackId).map { action =>
        val actionList = Seq(action)
        val actionsGroup = eventContext.messageAttachmentFor(maybeCallbackId = Some(callbackId), actions = actionList)
        TextWithAttachmentsResult(
          superPromptResult.event,
          superPromptResult.maybeConversation,
          superPromptResult.fullText,
          superPromptResult.responseType,
          Seq(actionsGroup)
        )
      }.getOrElse(superPromptResult)
    }
  }

}

object TextType extends BuiltInTextualType {
  val name = "Text"

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful("String")

  val typescriptType: String = "string"

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

  val invalidValueText: String = "I need a valid answer."

}

object NumberType extends BuiltInTextualType {
  val name = "Number"

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful("Float")

  val typescriptType: String = "number"

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

  val invalidValueText: String = "I need a number to answer this."
}

object DateTimeType extends BuiltInTextualType {
  val name: String = "Date & Time"

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful("String")

  val typescriptType: String = "string"

  override val mayRequireTypedAnswer: Boolean = true

  override def questionTextFor(context: BehaviorParameterContext, paramCount: Int, maybeRoot: Option[ParentConversation]): String = {
    val tz = context.behaviorVersion.team.timeZone.getDisplayName(TextStyle.FULL, Locale.getDefault(Locale.Category.DISPLAY))
    super.questionTextFor(context, paramCount, maybeRoot) ++ s""" ($tz)"""
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

  val invalidValueText: String = "I need something I can interpret as a date & time to answer this."

}

object YesNoType extends BuiltInType {
  val name = "Yes/No"
  val yesStrings = Seq("y", "yes", "yep", "yeah", "t", "true", "sure", "why not")
  val noStrings = Seq("n", "no", "nope", "nah", "f", "false", "no way", "no chance")

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful("Boolean")

  val typescriptType: String = "boolean"

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

  val invalidValueText: String = "I need an answer like “yes” or “no”."

  override def promptResultForAction(
                                     maybePreviousCollectedValue: Option[String],
                                     context: BehaviorParameterContext,
                                     paramState: ParamCollectionState,
                                     isReminding: Boolean
                                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    super.promptResultForAction(maybePreviousCollectedValue, context, paramState, isReminding).map { superPromptResult =>
      val callbackId = context.yesNoCallbackId
      val eventContext = context.event.eventContext
      val actionList = Seq(
        eventContext.messageActionButtonFor(callbackId, "Yes", YES),
        eventContext.messageActionButtonFor(callbackId, "No", NO)
      )
      val actionsGroup = eventContext.messageAttachmentFor(maybeCallbackId = Some(callbackId), actions = actionList)
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

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = DBIO.successful("File")

  val typescriptType: String =
    """{
      |  id: string,
      |  fetch: () => Promise<{
      |    value: any,
      |    contentType: string,
      |    filename: string
      |  }>
      |} | null
    """.stripMargin

  override val mayRequireTypedAnswer: Boolean = true

  def isIntentionallyEmpty(text: String): Boolean = text.trim.toLowerCase == "none"

  override def questionTextFor(context: BehaviorParameterContext, paramCount: Int, maybeRoot: Option[ParentConversation]): String = {
    super.questionTextFor(context, paramCount, maybeRoot) ++ """ (or type "none" if you don't have one)"""
  }

  private def alreadyHasFile(text: String, context: BehaviorParameterContext)(implicit ec: ExecutionContext): Future[Boolean] = {
    context.services.fileMap.maybeUrlFor(text).map(_.nonEmpty)
  }

  def isValidAction(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    DBIO.from(alreadyHasFile(text, context)).map { alreadyHasFile =>
      alreadyHasFile || context.event.hasFile || isIntentionallyEmpty(text)
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
      case e: MessageEvent => e.maybeNewFileId(context.services).getOrElse(super.potentialValueFor(event, context))
      case _ => super.potentialValueFor(event, context)
    }
  }

  override def decorationCodeFor(param: BehaviorParameter): String = {
    val paramName = param.input.name;
    raw"""if ($paramName) { $paramName.fetch = require("$FETCH_FUNCTION_FOR_FILE_PARAM_NAME")($paramName, $CONTEXT_PARAM); }"""
  }

  val invalidValueText: String = raw"""I need you to upload a file or type `none` if you don’t have one."""
}

case class ValidValue(id: String, label: String, data: JsObject)

case class BehaviorBackedDataType(dataTypeConfig: DataTypeConfig) extends BehaviorParameterType {

  val name = dataTypeConfig.behaviorVersion.maybeName.getOrElse("Unnamed data type")
  val id = dataTypeConfig.behaviorVersion.id
  val isBuiltIn: Boolean = false
  override val mayRequireTypedAnswer: Boolean = true

  def exportId(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    load(dataService).map(_.exportId)
  }

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    load(dataService).map(_.outputName)
  }

  override def inputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    load(dataService).map(_.inputName)
  }

  val typescriptType: String = BehaviorParameterType.typescriptTypeForDataTypes

  def load(dataService: DataService)(implicit ec: ExecutionContext): DBIO[LoadedBehaviorBackedDataType] = {
    LoadedBehaviorBackedDataType.fromAction(this, dataService)
  }

  def load(context: BehaviorParameterContext)(implicit ec: ExecutionContext): DBIO[LoadedBehaviorBackedDataType] = {
    load(context.dataService)
  }

  def resolvedValueForAction(
                              text: String,
                              context: BehaviorParameterContext
                            )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]] = {
    load(context).flatMap(_.resolvedValueForAction(text, context))
  }

  def isValidAction(
                     text: String,
                     context: BehaviorParameterContext
                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    load(context).flatMap(_.isValidAction(text, context))
  }

  def prepareForInvocation(
                            text: String,
                            context: BehaviorParameterContext
                          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[JsValue] = {
    load(context).flatMap(_.prepareForInvocation(text, context))
  }

  def needsConfigAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Boolean] = {
    load(dataService).flatMap(_.needsConfigAction(dataService))
  }

  override def validValuesFor(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    load(context).map(_.validValuesFor(result, context))
  }

  override def promptResultWithValidValuesResult(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    load(context).flatMap(_.promptResultWithValidValuesResult(result, context))
  }

  override def promptResultForAction(
                                      maybePreviousCollectedValue: Option[String],
                                      context: BehaviorParameterContext,
                                      paramState: ParamCollectionState,
                                      isReminding: Boolean
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    load(context).flatMap(_.promptResultForAction(maybePreviousCollectedValue, context, paramState, isReminding))
  }

  override def handleCollectedAction(
                                      event: Event,
                                      paramState: ParamCollectionState,
                                      context: BehaviorParameterContext
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Unit] = {
    load(context).flatMap(_.handleCollectedAction(event, paramState, context))
  }

  def superHandleCollectedAction(
                                  event: Event,
                                  paramState: ParamCollectionState,
                                  context: BehaviorParameterContext
                                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Unit] = {
    super.handleCollectedAction(event, paramState, context)
  }

  val invalidValueText: String = s"I need a $name. Choose one of the options below."

}

object BehaviorParameterType {

  val ID_PROPERTY = "id"
  val LABEL_PROPERTY = "label"
  val DATA_PROPERTY = "data"
  val SEARCH_COUNT_THRESHOLD = 30
  val otherId: String = "other"

  val typescriptTypeForDataTypes: String = {
    """{
      |  id: string,
      |  label: string,
      |  [k: string]: any
      |}
    """.stripMargin
  }

  val allBuiltin = Seq(
    TextType,
    NumberType,
    YesNoType,
    DateTimeType,
    FileType
  )

  def findBuiltIn(id: String): Option[BehaviorParameterType] = allBuiltin.find(_.id == id)

  def dataTypeConfigsFor(
                          maybeBehaviorGroupVersion: Option[BehaviorGroupVersion],
                          dataService: DataService
                        )(implicit ec: ExecutionContext): DBIO[Seq[DataTypeConfig]] = {
    maybeBehaviorGroupVersion.map { groupVersion =>
      dataService.dataTypeConfigs.allForAction(groupVersion)
    }.getOrElse(DBIO.successful(Seq()))
  }

  def allForAction(maybeBehaviorGroupVersion: Option[BehaviorGroupVersion], dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[BehaviorParameterType]] = {
    dataTypeConfigsFor(maybeBehaviorGroupVersion, dataService).map { dataTypeConfigs =>
      allBuiltin ++ dataTypeConfigs.map(BehaviorBackedDataType.apply)
    }
  }

  def findAction(id: String, behaviorGroupVersion: BehaviorGroupVersion, dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[BehaviorParameterType]] = {
    allBuiltin.find(_.id == id).map(pt => DBIO.successful(Some(pt))).getOrElse {
      dataTypeConfigsFor(Some(behaviorGroupVersion), dataService).map { dataTypeConfigs =>
        dataTypeConfigs.find(_.behaviorVersion.id == id).map(BehaviorBackedDataType.apply)
      }
    }
  }

}
