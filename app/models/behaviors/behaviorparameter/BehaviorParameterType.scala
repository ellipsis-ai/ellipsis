package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, SlackMessageEvent}
import models.behaviors.{BotResult, ParameterValue, ParameterWithValue, SuccessResult}
import models.team.Team
import play.api.libs.json._
import services.{AWSLambdaConstants, DataService}
import slick.dbio.DBIO
import utils.SlackMessageReactionHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

sealed trait BehaviorParameterType {

  val id: String
  val exportId: String
  val name: String
  def needsConfig(dataService: DataService): Future[Boolean]

  def isValid(text: String, context: BehaviorParameterContext): Future[Boolean]

  def prepareForInvocation(text: String, context: BehaviorParameterContext): Future[JsValue]

  def invalidPromptModifier: String

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[String]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      s" (${invalidPromptModifier})"
    } else {
      ""
    }
  }

  def promptFor(
                 maybePreviousCollectedValue: Option[String],
                 context: BehaviorParameterContext,
                 paramState: ParamCollectionState,
                 isReminding: Boolean
               ): Future[String] = {
    for {
      isFirst <- context.isFirstParam
      paramCount <- context.unfilledParamCount(paramState)
    } yield {
      val preamble = if (isReminding || !isFirst || paramCount <= 1) {
        ""
      } else {
        context.event.messageRecipientPrefix ++ (if (paramCount == 2) {
          s"I need to ask you a couple of questions."
        } else if (paramCount < 5) {
          s"I need to ask you a few questions."
        } else {
          s"I need to ask you some questions."
        })
      }
      s"$preamble\n\n**${context.parameter.question}** ${invalidValueModifierFor(maybePreviousCollectedValue)}"
    }
  }

  def resolvedValueFor(text: String, context: BehaviorParameterContext): Future[Option[String]]

  def handleCollected(event: Event, context: BehaviorParameterContext): Future[Unit] = {
    val potentialValue = event.relevantMessageText
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

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
  lazy val exportId = name
  def needsConfig(dataService: DataService) = Future.successful(false)
  def resolvedValueFor(text: String, context: BehaviorParameterContext): Future[Option[String]] = {
    Future.successful(Some(text))
  }
}

object TextType extends BuiltInType {
  val name = "Text"

  def isValid(text: String, context: BehaviorParameterContext) = Future.successful(true)

  def prepareForInvocation(text: String, context: BehaviorParameterContext) = Future.successful(JsString(text))

  val invalidPromptModifier: String = "I need a valid answer"

}

object NumberType extends BuiltInType {
  val name = "Number"

  def isValid(text: String, context: BehaviorParameterContext) = Future.successful {
    try {
      text.toDouble
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareForInvocation(text: String, context: BehaviorParameterContext) = Future.successful {
    try {
      JsNumber(BigDecimal(text))
    } catch {
      case e: NumberFormatException => JsString(text)
    }
  }

  val invalidPromptModifier: String = "I need a number"
}

object YesNoType extends BuiltInType {
  val name = "Yes/No"
  val yesStrings = Seq("y", "yes", "yep", "yeah", "t", "true", "sure", "why not")
  val noStrings = Seq("n", "no", "nope", "nah", "f", "false", "no way", "no chance")

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

  def isValid(text: String, context: BehaviorParameterContext): Future[Boolean] = {
    Future.successful(maybeValidValueFor(text).isDefined)
  }

  def prepareForInvocation(text: String, context: BehaviorParameterContext) = Future.successful {
    maybeValidValueFor(text).map { vv =>
      JsBoolean(vv)
    }.getOrElse(JsString(text))
  }

  val invalidPromptModifier: String = "I need something like 'yes' or 'no'"
}


case class BehaviorBackedDataType(behaviorVersion: BehaviorVersion) extends BehaviorParameterType {

  val id = behaviorVersion.id
  override val exportId: String = behaviorVersion.behavior.maybeExportId.getOrElse(id)
  val name = behaviorVersion.maybeName.getOrElse("Unnamed data type")

  case class ValidValue(id: String, label: String, data: Map[String, String])
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

  def resolvedValueFor(text: String, context: BehaviorParameterContext): Future[Option[String]] = {
    cachedValidValueFor(text, context).map { vv =>
      Future.successful(Some(vv))
    }.getOrElse {
      fetchMatchFor(text, context)
    }.map { maybeValidValue =>
      maybeValidValue.map(v => Json.toJson(v).toString)
    }
  }

  def editLinkFor(context: BehaviorParameterContext) = {
    val behavior = behaviorVersion.behavior
    val link = context.dataService.behaviors.editLinkFor(behavior.group.id, Some(behavior.id), context.configuration)
    s"[${context.parameter.paramType.name}]($link)"
  }

  def needsConfig(dataService: DataService) = {
    for {
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion.groupVersion)
    } yield !requiredOAuth2ApiConfigs.forall(_.isReady)
  }

  val team = behaviorVersion.team

  def maybeValidValueForSavedAnswer(value: ValidValue, context: BehaviorParameterContext): Future[Option[ValidValue]] = {
    usesSearch(context).flatMap { usesSearch =>
      if (usesSearch) {
        fetchValidValues(Some(value.label), context).map { values =>
          values.find(_.id == value.id)
        }
      } else {
        fetchMatchFor(value.id, context)
      }
    }
  }

  def isValid(text: String, context: BehaviorParameterContext) = {
    maybeValidValueFor(text, context).map(_.isDefined)
  }

  def maybeValidValueFor(text: String, context: BehaviorParameterContext): Future[Option[ValidValue]] = {
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
      cachedValidValueFor(text, context).map { v =>
        Future.successful(Some(v))
      }.getOrElse {
        fetchMatchFor(text, context)
      }
    }
  }

  private def cachedValuesFor(context: BehaviorParameterContext): Option[Seq[ValidValue]] = {
    for {
      conversation <- context.maybeConversation
      values <- context.cache.get[Seq[ValidValue]](valuesListCacheKeyFor(conversation, context.parameter))
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

  def prepareForInvocation(text: String, context: BehaviorParameterContext) = {
    maybeValidValueFor(text, context).map { maybeValidValue =>
      maybeValidValue.map { vv =>
        JsObject(Map(BehaviorParameterType.ID_PROPERTY -> JsString(vv.id), BehaviorParameterType.LABEL_PROPERTY -> JsString(vv.label)) ++ vv.data.map { case(k, v) => k -> JsString(v) })
      }.getOrElse(JsString(text))
    }
  }

  val invalidPromptModifier: String = s"I need a $name. Type one of the numbers or labels below or `...stop` to end this conversation."

  private def valuesListCacheKeyFor(conversation: Conversation, parameter: BehaviorParameter): String = {
    s"values-list-${conversation.id}-${parameter.id}"
  }

  private def searchQueryCacheKeyFor(conversation: Conversation, parameter: BehaviorParameter): String = {
    s"search-query-${conversation.id}-${parameter.id}"
  }

  private def fetchValidValuesResult(maybeSearchQuery: Option[String], context: BehaviorParameterContext): Future[Option[BotResult]] = {
    val paramsWithValues = maybeSearchQuery.map { searchQuery =>
      val value = ParameterValue(searchQuery, JsString(searchQuery), isValid=true)
      Seq(ParameterWithValue(context.parameter, AWSLambdaConstants.invocationParamFor(0), Some(value)))
    }.getOrElse(Seq())
    for {
      maybeResult <- context.dataService.behaviorVersions.resultFor(behaviorVersion, paramsWithValues, context.event, context.maybeConversation).map(Some(_))
    } yield maybeResult
  }

  private def extractValidValueFrom(json: JsValue): Option[ValidValue] = {
    json.validate[ValidValue] match {
      case JsSuccess(data, _) => Some(data)
      case e: JsError => None
    }
  }

  private def extractValidValues(result: SuccessResult): Seq[ValidValue] = {
    result.result.as[Seq[JsObject]].flatMap { ea =>
      extractValidValueFrom(ea)
    }
  }

  private def fetchValidValues(maybeSearchQuery: Option[String], context: BehaviorParameterContext): Future[Seq[ValidValue]] = {
    fetchValidValuesResult(maybeSearchQuery, context).map { maybeResult =>
      maybeResult.map {
        case r: SuccessResult => extractValidValues(r)
        case r: BotResult => Seq()
      }.getOrElse(Seq())
    }
  }

  private def textMatchesLabel(text: String, label: String, context: BehaviorParameterContext): Boolean = {
    val lowercaseText = text.toLowerCase
    val unformattedText = context.event.unformatTextFragment(text).toLowerCase
    val lowercaseLabel = label.toLowerCase
    lowercaseLabel == lowercaseText || lowercaseLabel == unformattedText
  }

  private def fetchMatchFor(text: String, context: BehaviorParameterContext): Future[Option[ValidValue]] = {
    fetchValidValues(None, context).map { validValues =>
      validValues.find {
        v => v.id == text || textMatchesLabel(text, v.label, context)
      }
    }
  }

  private def cancelAndRespondFor(response: String, context: BehaviorParameterContext): Future[String] = {
    context.dataService.conversations.cancel(context.maybeConversation).map { _ =>
      s"$response\n\n(Your current action has been cancelled. Try again after fixing the problem.)"
    }
  }

  private def promptForListAllCase(
                                    maybeSearchQuery: Option[String],
                                    maybePreviousCollectedValue: Option[String],
                                    context: BehaviorParameterContext,
                                    paramState: ParamCollectionState,
                                    isReminding: Boolean
                                  ): Future[String] = {
    for {
      superPrompt <- maybeSearchQuery.map { searchQuery =>
        Future.successful(s"Here are some options for `$searchQuery`. Type a number to choose an option.")
      }.getOrElse(super.promptFor(maybePreviousCollectedValue, context, paramState, isReminding))
      maybeValidValuesResult <- fetchValidValuesResult(maybeSearchQuery, context)
      output <- maybeValidValuesResult.map {
        case r: SuccessResult => {
          val validValues = extractValidValues(r)
          if (validValues.isEmpty) {
            maybeSearchQuery.map { searchQuery =>
              val key = searchQueryCacheKeyFor(context.maybeConversation.get, context.parameter)
              context.cache.remove(key)
              Future.successful(s"I couldn't find anything matching `$searchQuery`. Try searching again or type `…stop`.")
            }.getOrElse {
              cancelAndRespondFor(s"This data type isn't returning any values: ${editLinkFor(context)}", context)
            }
          } else {
            context.maybeConversation.foreach { conversation =>
              context.cache.set(valuesListCacheKeyFor(conversation, context.parameter), validValues)
            }
            val valuesPrompt = validValues.zipWithIndex.map { case (ea, i) =>
              s"\n\n$i. ${ea.label}"
            }.mkString
            Future.successful(superPrompt ++ valuesPrompt)
          }
        }
        case r: BotResult => cancelAndRespondFor(r.fullText, context)
      }.getOrElse {
        cancelAndRespondFor(s"This data type appears to be misconfigured: ${editLinkFor(context)}", context)
      }
    } yield output

  }

  private def maybeCachedSearchQueryFor(context: BehaviorParameterContext): Option[String] = {
    context.maybeConversation.map { conversation =>
      context.cache.get[String](searchQueryCacheKeyFor(conversation, context.parameter))
    }.getOrElse(None)
  }

  private def promptForSearchCase(
                                   maybePreviousCollectedValue: Option[String],
                                   context: BehaviorParameterContext,
                                   paramState: ParamCollectionState,
                                   isReminding: Boolean
                                 ): Future[String] = {

    maybeCachedSearchQueryFor(context).map { searchQuery =>
      promptForListAllCase(Some(searchQuery), maybePreviousCollectedValue, context, paramState, isReminding)
    }.getOrElse {
      super.promptFor(maybePreviousCollectedValue, context, paramState, isReminding).map { superPrompt =>
        superPrompt ++ " Enter a search query:"
      }
    }
  }

  private def usesSearch(context: BehaviorParameterContext): Future[Boolean] = {
    context.dataService.behaviorVersions.hasSearchParam(behaviorVersion)
  }

  override def promptFor(
                           maybePreviousCollectedValue: Option[String],
                           context: BehaviorParameterContext,
                           paramState: ParamCollectionState,
                           isReminding: Boolean
                         ): Future[String] = {
    val eventualPrompt = usesSearch(context).flatMap { usesSearch =>
      if (usesSearch) {
        promptForSearchCase(maybePreviousCollectedValue, context, paramState, isReminding)
      } else {
        promptForListAllCase(None, maybePreviousCollectedValue, context, paramState, isReminding)
      }
    }
    context.event match {
      case event: SlackMessageEvent => {
        implicit val actorSystem = context.actorSystem
        SlackMessageReactionHandler.handle(event.clientFor(context.dataService), eventualPrompt, event.channel, event.ts)
      }
      case _ =>
    }

    eventualPrompt

  }

  override def handleCollected(event: Event, context: BehaviorParameterContext): Future[Unit] = {
    usesSearch(context).flatMap { usesSearch =>
      if (usesSearch && maybeCachedSearchQueryFor(context).isEmpty && context.maybeConversation.isDefined) {
        val key = searchQueryCacheKeyFor(context.maybeConversation.get, context.parameter)
        val searchQuery = event.relevantMessageText
        context.cache.set(key, searchQuery, 5.minutes)
        Future.successful({})
      } else {
        super.handleCollected(event, context)
      }
    }
  }

}

object BehaviorParameterType {

  val ID_PROPERTY = "id"
  val LABEL_PROPERTY = "label"
  val DATA_PROPERTY = "data"

  val allBuiltin = Seq(
    TextType,
    NumberType,
    YesNoType
  )

  def findBuiltIn(id: String): Option[BehaviorParameterType] = allBuiltin.find(_.id == id)

  def allFor(team: Team, dataService: DataService): Future[Seq[BehaviorParameterType]] = {
    dataService.behaviorVersions.dataTypesForTeam(team).map { behaviorBacked =>
      allBuiltin ++ behaviorBacked.map(BehaviorBackedDataType.apply)
    }
  }

  def allForAction(maybeBehaviorGroupVersion: Option[BehaviorGroupVersion], dataService: DataService): DBIO[Seq[BehaviorParameterType]] = {
    maybeBehaviorGroupVersion.map { groupVersion =>
      dataService.behaviorVersions.dataTypesForGroupVersionAction(groupVersion)
    }.getOrElse(DBIO.successful(Seq())).map { behaviorBacked =>
      allBuiltin ++ behaviorBacked.map(BehaviorBackedDataType.apply)
    }
  }

  def findAction(id: String, behaviorGroupVersion: BehaviorGroupVersion, dataService: DataService): DBIO[Option[BehaviorParameterType]] = {
    allForAction(Some(behaviorGroupVersion), dataService).map { all =>
      all.find {
        case paramType: BehaviorBackedDataType => paramType.id == id || paramType.behaviorVersion.maybeExportId.contains(id)
        case paramType: BehaviorParameterType => paramType.id == id
      }
    }
  }

}
