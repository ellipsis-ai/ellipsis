package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import models.behaviors._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.{NewParentConversation, ParentConversation}
import models.behaviors.datatypeconfig.DataTypeConfig
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import play.api.libs.json._
import services.AWSLambdaConstants._
import services.DataService
import services.caching.DataTypeBotResultsCacheKey
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

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean]

  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsValue]

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

  def questionTextFor(context: BehaviorParameterContext): String = {
    context.parameter.question.trim
  }

  def preambleFor(paramCount: Int, maybeParent: Option[ParentConversation]): String = {
    val iPart = maybeParent.map { p =>
      s"To be able to answer the question **${p.param.question}**, I first"
    }.getOrElse("I")
    val otherPart = maybeParent.map(_ => "other ").getOrElse("")
    (if (paramCount == 2) {
      s"$iPart need to ask you a couple of ${otherPart}questions."
    } else if (paramCount < 5) {
      s"$iPart need to ask you a few ${otherPart}questions."
    } else {
      s"$iPart need to ask you some ${otherPart}questions."
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
        DBIO.from(context.unfilledParamCount(paramState))
      }.getOrElse(DBIO.successful(0))
      maybeParent <- context.maybeConversation.map { convo =>
        context.dataService.parentConversations.maybeForAction(convo)
      }.getOrElse(DBIO.successful(None))
    } yield {
      val prefix = context.event.messageRecipientPrefix
      val invalidModifier = invalidValueModifierFor(maybePreviousCollectedValue)
      val preamble = if (isReminding || !isFirst || paramCount <= 1 || invalidModifier.nonEmpty) {
        ""
      } else {
        preambleFor(paramCount, maybeParent)
      }
      s"""$prefix$preamble$invalidModifier __${questionTextFor(context)}__"""
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

  def resolvedValueFor(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def potentialValueFor(event: Event, context: BehaviorParameterContext): String = event.relevantMessageText

  def handleCollected(event: Event, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val potentialValue = potentialValueFor(event, context)
    val input = context.parameter.input
    if (input.isSaved) {
      resolvedValueFor(potentialValue, context).flatMap { maybeValueToSave =>
        maybeValueToSave.map { valueToSave =>
          event.ensureUser(context.dataService).flatMap { user =>
            context.dataService.savedAnswers.ensureFor(input, valueToSave, user).map(_ => {})
          }
        }.getOrElse(Future.successful({}))
      }
    } else {
      context.maybeConversation.map { conversation =>
        context.dataService.collectedParameterValues.ensureFor(context.parameter, conversation, potentialValue).map(_ => {})
      }.getOrElse(Future.successful({}))
    }
  }

  def decorationCodeFor(param: BehaviorParameter): String = ""

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
  lazy val exportId = name
  val isBuiltIn: Boolean = true
  def needsConfig(dataService: DataService)(implicit ec: ExecutionContext) = Future.successful(false)
  def resolvedValueFor(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(Some(text))
  }
  def prepareValue(text: String): JsValue
  def prepareJsValue(value: JsValue): JsValue
  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful(prepareValue(text))
}

object TextType extends BuiltInType {
  val name = "Text"

  val outputName: String = "String"

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful(true)

  def prepareValue(text: String) = JsString(text)
  def prepareJsValue(value: JsValue): JsValue = {
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

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful {
    try {
      text.toDouble
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareValue(text: String): JsValue = try {
    JsNumber(BigDecimal(text))
  } catch {
    case e: NumberFormatException => JsString(text)
  }

  def prepareJsValue(value: JsValue): JsValue = {
    value match {
      case n: JsNumber => n
      case s: JsString => prepareValue(s.value)
      case v => v
    }
  }

  val invalidPromptModifier: String = s"I need a number to answer this. $stopInstructions"
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

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean] = {
    Future.successful(maybeValidValueFor(text).isDefined)
  }

  def prepareValue(text: String) = {
    maybeValidValueFor(text).map { vv =>
      JsBoolean(vv)
    }.getOrElse(JsString(text))
  }

  def prepareJsValue(value: JsValue): JsValue = {
    value match {
      case b: JsBoolean => b
      case s: JsString => prepareValue(s.value)
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
        superPromptResult.forcePrivateResponse,
        Seq(actionsGroup)
      )
    }
  }
}

object FileType extends BuiltInType {
  val name = "File"

  val outputName: String = "File"

  def isIntentionallyEmpty(text: String): Boolean = text.trim.toLowerCase == "none"

  override def questionTextFor(context: BehaviorParameterContext): String = {
    super.questionTextFor(context) ++ """ (or type "none" if you don't have one)"""
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

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean] = {
    Future.successful {
      alreadyHasFile(text, context) || eventHasFile(context) || isIntentionallyEmpty(text)
    }
  }

  def prepareValue(text: String) = {
    if (isIntentionallyEmpty(text)) {
      JsNull
    } else {
      Json.toJson(Map("id" -> text))
    }
  }

  def prepareJsValue(value: JsValue): JsValue = {
    value match {
      case s: JsString => prepareValue(s.value)
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

case class ValidValue(id: String, label: String, data: Map[String, String])

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
            obj.value.
              filterNot { case(k, _) => k == BehaviorParameterType.ID_PROPERTY || k == BehaviorParameterType.LABEL_PROPERTY }.
              map { case(k, v) => (k, v.as[String]) }.
              toMap
          }
          case _ => Map[String, String]()
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
      ) ++ vv.data.map { case(k, v) => (k, JsString(v)) }
    )
  }

  def resolvedValueFor(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    cachedValidValueFor(text, context).map { vv =>
      Future.successful(Some(vv))
    }.getOrElse {
      getMatchFor(text, context)
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

  def maybeValidValueForSavedAnswer(value: ValidValue, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[ValidValue]] = {
    getMatchFor(value.id, context)
  }

  def isValid(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = {
    maybeValidValueFor(text, context).map(_.isDefined)
  }

  def maybeValidValueFor(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[ValidValue]] = {
    val maybeJson = try {
      Some(Json.parse(text))
    } catch {
      case e: JsonParseException => None
    }
    maybeJson.flatMap { json =>
      extractValidValueFrom(json).map { validValue =>
        maybeValidValueForSavedAnswer(validValue, context)
      }
    }.getOrElse {
      if (isCollectingOther(context)) {
        Future.successful(Some(ValidValue(BehaviorParameterType.otherId, text, Map())))
      } else {
        cachedValidValueFor(text, context).map { v =>
          Future.successful(Some(v))
        }.getOrElse {
          getMatchFor(text, context)
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

  def prepareForInvocation(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = {
    maybeValidValueFor(text, context).map { maybeValidValue =>
      maybeValidValue.map { vv =>
        JsObject(Map(BehaviorParameterType.ID_PROPERTY -> JsString(vv.id), BehaviorParameterType.LABEL_PROPERTY -> JsString(vv.label)) ++ vv.data.map { case(k, v) => k -> JsString(v) })
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

  private def fetchValidValuesResultAction(
                                            context: BehaviorParameterContext
                                          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      behaviorResponse <- context.dataService.behaviorResponses.buildForAction(
        context.event,
        behaviorVersion,
        Map(),
        None,
        None,
        context.maybeConversation.map(c => NewParentConversation(c, context.parameter))
      )
      result <- DBIO.from(behaviorResponse.result)
    } yield result
  }

  private def dataTypeResultCacheKeyFor(context: BehaviorParameterContext)(implicit ec: ExecutionContext): DataTypeBotResultsCacheKey = {
    DataTypeBotResultsCacheKey(context.parameter.id, None, context.maybeConversation.map(_.id))
  }

  private def getValidValuesResultAction(
                                          context: BehaviorParameterContext
                                        )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      result <- DBIO.from(context.cacheService.getDataTypeBotResult(dataTypeResultCacheKeyFor(context), (key: DataTypeBotResultsCacheKey) => {
        context.dataService.run(fetchValidValuesResultAction(context))
      }))
    } yield result
  }

  private def extractValidValueFrom(json: JsValue): Option[ValidValue] = {
    json.validate[ValidValue] match {
      case JsSuccess(data, _) => Some(data)
      case _: JsError => None
    }
  }

  private def extractValidValues(result: SuccessResult): Seq[ValidValue] = {
    result.result.validate[Seq[JsObject]] match {
      case JsSuccess(data, _) => {
        data.flatMap { ea =>
          extractValidValueFrom(ea)
        }
      }
      case _: JsError => {
        result.result.validate[Seq[String]] match {
          case JsSuccess(strings, _) => strings.map { ea => ValidValue(ea, ea, Map()) }
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
      getValidValuesResultAction(context).map {
        case r: SuccessResult => extractValidValues(r)
        case r: BotResult => throw FetchValidValuesBadResultException(r)
      }
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

  private def getMatchFor(text: String, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[ValidValue]] = {
    getValidValues(Some(text), context).map { validValues =>
      validValues.find {
        v => v.id == text || textMatchesLabel(text, v.label, context)
      }
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

  private def promptResultWithValidValues(validValues: Seq[ValidValue], context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      output <- if (validValues.isEmpty) {
        context.services.dataService.behaviorParameters.allForAction(behaviorVersion).flatMap { params =>
          if (params.isEmpty) {
            cancelAndRespondForAction(s"This data type isn't returning any values: ${editLinkFor(context)}", context)
          } else {
            context.maybeConversation.map { convo =>
              DBIO.from(convo.resultFor(context.event, context.services))
            }.getOrElse(cancelAndRespondForAction(s"This data type isn't returning any values: ${editLinkFor(context)}", context))
          }
        }
      } else {
        context.maybeConversation.foreach { conversation =>
          context.cacheService.cacheValidValues(valuesListCacheKeyFor(conversation, context.parameter), validValues)
        }
        val builtinMenuItems = Seq(
          SlackMessageActionMenuItem(Conversation.START_AGAIN_MENU_ITEM_TEXT, Conversation.START_AGAIN_MENU_ITEM_TEXT),
          SlackMessageActionMenuItem(Conversation.CANCEL_MENU_ITEM_TEXT, Conversation.CANCEL_MENU_ITEM_TEXT)
        )
        val menuItems = validValues.zipWithIndex.map { case (ea, i) =>
          SlackMessageActionMenuItem(s"${i+1}. ${ea.label}", ea.label)
        } ++ builtinMenuItems
        val actionsList = Seq(SlackMessageActionMenu("ignored", "Choose an option", menuItems))
        val groups: Seq[MessageAttachmentGroup] = Seq(
          SlackMessageActionsGroup(context.dataTypeChoiceCallbackId, actionsList, None, None, Some(Color.BLUE_LIGHT))
        )
        promptTextForAction(None, context, None, false).map { text =>
          TextWithAttachmentsResult(context.event, context.maybeConversation, text, context.behaviorVersion.forcePrivateResponse, groups)
        }
      }
    } yield output
  }

  override def promptResultWithValidValuesResult(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    val validValues = result match {
      case r: SuccessResult => extractValidValues(r)
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
      for {
        initialResult <- getValidValuesResultAction(context)
        result <- if (isCollectingOther(context)) {
          promptResultForOtherCaseAction(context)
        } else if (initialResult.maybeConversation.isDefined) {
          DBIO.successful(initialResult)
        } else {
          promptResultWithValidValuesResult(initialResult, context)
        }
      } yield result
    } else {
      for {
        validValues <- validValuesFromDefaultStorageFor(None, context)
        result <- promptResultWithValidValues(validValues, context)
      } yield result
    }
  }

  override def handleCollected(event: Event, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    if (!isCollectingOther(context) && isOther(context) && context.maybeConversation.isDefined) {
      maybeCollectingOtherCacheKeyFor(context).foreach { key =>
        context.cacheService.set(key, "true", 5.minutes)
      }
      Future.successful({})
    } else if (isRequestingStartAgain(context)) {
      Future.successful(context.cacheService.clearDataTypeBotResult(dataTypeResultCacheKeyFor(context)))
    } else {
      super.handleCollected(event, context)
    }
  }

  def isOther(context: BehaviorParameterContext): Boolean = {
    cachedValidValueFor(context.event.relevantMessageText, context).exists { v =>
      v.id == BehaviorParameterType.otherId
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
