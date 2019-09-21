package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric
import models.behaviors.behaviorversion.Normal
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.datatypeconfig.LoadedDataTypeConfig
import models.behaviors.events.{Event, MessageAttachment}
import models.behaviors.{BotResult, SuccessResult, TextWithAttachmentsResult}
import play.api.libs.json._
import services.caching.DataTypeBotResultsCacheKey
import services.{AWSLambdaConstants, DataService}
import slick.dbio.DBIO
import utils.Color

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class LoadedBehaviorBackedDataType(dataType: BehaviorBackedDataType, dataTypeConfig: LoadedDataTypeConfig) {

  val behaviorVersion = dataTypeConfig.behaviorVersion

  val id = behaviorVersion.id
  val exportId: String = behaviorVersion.behavior.maybeExportId.getOrElse(id)

  val outputName: String = behaviorVersion.outputName

  lazy val inputName: String = behaviorVersion.inputName

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

  def resolvedValueForAction(
                              text: String,
                              context: BehaviorParameterContext
                            )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[String]] = {
    DBIO.from(cachedValidValueFor(text, context)).flatMap { maybeCachedVV =>
      maybeCachedVV.map { vv =>
        DBIO.successful(Some(vv))
      }.getOrElse {
        getMatchForAction(text, context)
      }.map { maybeValidValue =>
        maybeValidValue.map(v => Json.toJson(v).toString)
      }
    }
  }

  def editLinkFor(context: BehaviorParameterContext) = {
    val behavior = behaviorVersion.behavior
    val link = context.dataService.behaviors.editLinkFor(behavior.group.id, Some(behavior.id), context.services.configuration)
    s"[${context.parameter.paramType.name}]($link)"
  }

  def needsConfigAction(dataService: DataService)(implicit ec: ExecutionContext) = {
    for {
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(behaviorVersion.groupVersion)
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

  def maybeValidValuesForAction(
                                 text: String,
                                 context: BehaviorParameterContext
                               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    maybeValidValueForText(text).map { validValue =>
      maybeValidValueForSavedAnswerAction(validValue, context).map(_.toSeq)
    }.getOrElse {
      DBIO.from(isCollectingOther(context)).flatMap { isCollectingOther =>
        if (isCollectingOther) {
          DBIO.successful(Seq(ValidValue(BehaviorParameterType.otherId, text, Json.obj())))
        } else {
          DBIO.from(cachedValidValueFor(text, context)).flatMap { maybeCachedVV =>
            maybeCachedVV.map { v =>
              DBIO.successful(Seq(v))
            }.getOrElse {
              getValidValuesAction(Some(text), context)
            }
          }
        }
      }
    }
  }

  private def cachedValuesFor(context: BehaviorParameterContext): Future[Option[Seq[ValidValue]]] = {
    context.maybeConversation.map { conversation =>
      context.cacheService.getValidValues(valuesListCacheKeyFor(conversation, context.parameter))
    }.getOrElse(Future.successful(None))
  }

  private def cachedValidValueAtIndex(
                                       text: String,
                                       context: BehaviorParameterContext
                                     )(implicit ec: ExecutionContext): Future[Option[ValidValue]] = {
    cachedValuesFor(context).map { maybeValues =>
      maybeValues.flatMap { values =>
        try {
          val index = text.toInt - 1
          Some(values(index))
        } catch {
          case e: NumberFormatException => None
          case e: IndexOutOfBoundsException => None
        }
      }
    }
  }

  private def cachedValidValueForLabel(
                                        text: String,
                                        context: BehaviorParameterContext
                                      )(implicit ec: ExecutionContext): Future[Option[ValidValue]] = {
    cachedValuesFor(context).map { maybeValues =>
      maybeValues.flatMap { values =>
        values.find((ea) => textMatchesLabel(text, ea.label, context))
      }
    }
  }

  private def cachedValidValueFor(
                                   text: String,
                                   context: BehaviorParameterContext
                                 )(implicit ec: ExecutionContext): Future[Option[ValidValue]] = {
    cachedValidValueAtIndex(text, context).flatMap { maybeAtIndex =>
      maybeAtIndex.map(v => Future.successful(Some(v))).getOrElse {
        cachedValidValueForLabel(text, context)
      }
    }
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
        None,
        userExpectsResponse = true,
        maybeMessageListener = None
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
      Some(behaviorVersion),
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

  private def extractSingleValidValue(
                                       result: SuccessResult,
                                       context: BehaviorParameterContext
                                     )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Option[ValidValue] = {
    result.maybeLogResult.foreach { logResult =>
      if (result.shouldIncludeLogs && logResult.authorDefinedLogStatements.nonEmpty) {
        sendLogMessageInBackgroundFor(result, context)
      }
    }
    result.result.validate[JsObject] match {
      case JsSuccess(data, _) => {
        extractValidValueFrom(data)
      }
      case _: JsError => {
        result.result.validate[String] match {
          case JsSuccess(string, _) => Some(ValidValue(string, string, Json.obj()))
          case _: JsError => None
        }
      }
    }
  }

  private def validValuesFromDefaultStorageFor(
                                                maybeMatchText: Option[String],
                                                context: BehaviorParameterContext
                                              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[ValidValue]] = {
    for {
      maybeLabelField <- context.dataService.dataTypeFields.allForAction(dataTypeConfig.config).map{ fields =>
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
      getValidValuesSuccessResultAction(maybeMatchText, context).map { res =>
        maybeSingleValidValueFor(res, context).map(Seq(_)).getOrElse {
          validValuesFor(res, context)
        }
      }
    } else {
      validValuesFromDefaultStorageFor(maybeMatchText, context)
    }
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

  private def promptResultWithSimpleValidValues(
                                                 validValues: Seq[ValidValue],
                                                 context: BehaviorParameterContext,
                                                 params: Seq[BehaviorParameter]
                                               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    dataType.promptTextForAction(None, context, None, false).map { text =>
      val callbackId = context.dataTypeChoiceCallbackId
      val eventContext = context.event.eventContext
      val actionList = validValues.map { ea =>
        eventContext.messageActionButtonFor(callbackId, ea.label, ea.label)
      } ++ Seq(
        Some(eventContext.messageActionButtonFor(callbackId, Conversation.CANCEL_MENU_ITEM_TEXT, Conversation.CANCEL_MENU_ITEM_TEXT))
      ).flatten
      val attachment = eventContext.messageAttachmentFor(maybeCallbackId = Some(callbackId), actions = actionList)
      TextWithAttachmentsResult(
        context.event,
        context.maybeConversation,
        text,
        context.behaviorVersion.responseType,
        Seq(attachment)
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

  private def promptResultWithSingleValidValue(validValue: ValidValue, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    val conversation = context.maybeConversation.get
    for {
      _ <- DBIO.from(context.cacheService.cacheValidValues(valuesListCacheKeyFor(conversation, context.parameter), Seq(validValue)))
      _ <- context.services.dataService.collectedParameterValues.ensureForAction(context.parameter, conversation, validValue.label)
      updated <- conversation.updateToNextStateAction(context.event, context.services)
      res <- updated.respondAction(context.event, isReminding = false, context.services)
    } yield res
  }

  private def promptResultWithValidValues(validValues: Seq[ValidValue], context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      params <- context.services.dataService.behaviorParameters.allForAction(behaviorVersion)
      output <- if (validValues.isEmpty) {
        if (params.isEmpty) {
          cancelAndRespondForAction(s"This data type isn't returning any values: ${editLinkFor(context)}", context)
        } else {
          context.maybeConversation.map { convo =>
            context.dataService.collectedParameterValues.deleteForAction(context.parameter.id, convo).flatMap { _ =>
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
          val eventContext = context.event.eventContext
          val builtinMenuItems = Seq(
            Some(eventContext.messageActionMenuItemFor(Conversation.CANCEL_MENU_ITEM_TEXT, Conversation.CANCEL_MENU_ITEM_TEXT))
          ).flatten
          val menuItems = validValues.zipWithIndex.map { case (ea, i) =>
            eventContext.messageActionMenuItemFor(s"${i+1}. ${ea.label}", ea.label)
          } ++ builtinMenuItems
          val actionsList = Seq(eventContext.messageActionMenuFor(context.dataTypeChoiceCallbackId, "Choose an option", menuItems))
          val attachments: Seq[MessageAttachment] = Seq(
            eventContext.messageAttachmentFor(None, None, None, None, Some(Color.BLUE_LIGHT), Some(context.dataTypeChoiceCallbackId), actionsList)
          )
          dataType.promptTextForAction(None, context, None, false).map { text =>
            TextWithAttachmentsResult(context.event, context.maybeConversation, text, context.behaviorVersion.responseType, attachments)
          }
        }
      }
    } yield output
  }

  def validValuesFor(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Seq[ValidValue] = {
    result match {
      case r: SuccessResult => extractValidValues(r, context)
      case _ => Seq()
    }
  }

  def maybeSingleValidValueFor(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Option[ValidValue] = {
    result match {
      case r: SuccessResult => extractSingleValidValue(r, context)
      case _ => None
    }
  }

  def promptResultWithValidValuesResult(result: BotResult, context: BehaviorParameterContext)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    maybeSingleValidValueFor(result, context).map { single =>
      promptResultWithSingleValidValue(single, context)
    }.getOrElse(promptResultWithValidValues(validValuesFor(result, context), context))
  }

  private def isCollectingOther(context: BehaviorParameterContext): Future[Boolean] = {
    maybeCollectingOtherCacheKeyFor(context).map { key =>
      context.cacheService.hasKey(key)
    }.getOrElse(Future.successful(false))
  }

  def promptResultForAction(
                                      maybePreviousCollectedValue: Option[String],
                                      context: BehaviorParameterContext,
                                      paramState: ParamCollectionState,
                                      isReminding: Boolean
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    if (dataTypeConfig.usesCode) {
      DBIO.from(isCollectingOther(context)).flatMap { isCollectingOther =>
        if (isCollectingOther) {
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
          context.dataService.collectedParameterValues.deleteForAction(ea.parameterId, convo)
        })
      } yield {}
    }.getOrElse(DBIO.successful({}))
  }

  def handleCollectedAction(
                                      event: Event,
                                      paramState: ParamCollectionState,
                                      context: BehaviorParameterContext
                                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Unit] = {
    for {
      isCollectingOther <- DBIO.from(isCollectingOther(context))
      isOther <- DBIO.from(isOther(context))
      result <- {
        if (!isCollectingOther && isOther && context.maybeConversation.isDefined) {
          maybeCollectingOtherCacheKeyFor(context).foreach { key =>
            context.cacheService.set(key, "true", 5.minutes)
          }
          DBIO.successful({})
        } else if (isRequestingStartAgain(context)) {
          clearCollectedValuesForAction(context)
        } else {
          dataType.superHandleCollectedAction(event, paramState, context)
        }
      }
    } yield result
  }

  def isOther(context: BehaviorParameterContext)(implicit ec: ExecutionContext): Future[Boolean] = {
    cachedValidValueFor(context.event.relevantMessageText, context).map { maybeCached =>
      maybeCached.exists { v =>
        v.id.toLowerCase == BehaviorParameterType.otherId
      }
    }
  }

  def isRequestingStartAgain(context: BehaviorParameterContext): Boolean = {
    context.event.relevantMessageText == Conversation.START_AGAIN_MENU_ITEM_TEXT
  }

}

object LoadedBehaviorBackedDataType {
  def fromAction(dataType: BehaviorBackedDataType, dataService: DataService)(implicit ec: ExecutionContext): DBIO[LoadedBehaviorBackedDataType] = {
    LoadedDataTypeConfig.fromAction(dataType.dataTypeConfig, dataService).map { config =>
      LoadedBehaviorBackedDataType(dataType, config)
    }
  }
}
