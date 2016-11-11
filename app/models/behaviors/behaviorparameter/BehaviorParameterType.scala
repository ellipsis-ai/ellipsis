package models.behaviors.behaviorparameter

import models.behaviors.{BotResult, ParameterValue, ParameterWithValue, SuccessResult}
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.team.Team
import play.api.libs.json._
import services.{AWSLambdaConstants, DataService}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait BehaviorParameterType {

  val id: String
  val name: String
  def needsConfig(dataService: DataService): Future[Boolean]

  def isValid(text: String, context: BehaviorParameterContext): Future[Boolean]

  def prepareForInvocation(text: String, context: BehaviorParameterContext): Future[JsValue]

  def invalidPromptModifier: String

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[CollectedParameterValue]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      s" (${invalidPromptModifier})"
    } else {
      ""
    }
  }

  def promptFor(
                 maybePreviousCollectedValue: Option[CollectedParameterValue],
                 context: BehaviorParameterContext
               ): Future[String] = {
    Future.successful(s"${context.parameter.question}${invalidValueModifierFor(maybePreviousCollectedValue)}")
  }

  def handleCollected(event: MessageEvent, context: BehaviorParameterContext): Future[Unit] = {
    val potentialValue = event.context.relevantMessageText
    context.maybeConversation.map { conversation =>
      context.dataService.collectedParameterValues.ensureFor(context.parameter, conversation, potentialValue).map(_ => {})
    }.getOrElse(Future.successful({}))
  }

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
  def needsConfig(dataService: DataService) = Future.successful(false)
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


case class BehaviorBackedDataType(behavior: Behavior) extends BehaviorParameterType {

  val id = behavior.id
  val name = behavior.maybeDataTypeName.getOrElse("Unnamed data type")

  def editLinkFor(context: BehaviorParameterContext) = {
    val link = behavior.editLinkFor(context.configuration)
    s"[${context.parameter.paramType.name}]($link)"
  }

  def needsConfig(dataService: DataService) = {
    for {
      maybeCurrentVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      requiredApiConfigs <- maybeCurrentVersion.map { currentVersion =>
        dataService.requiredOAuth2ApiConfigs.allFor(currentVersion)
      }.getOrElse(Future.successful(Seq()))
    } yield !requiredApiConfigs.forall(_.isReady)
  }

  case class ValidValue(id: String, label: String)
  implicit val validValueReads = Json.reads[ValidValue]

  val team = behavior.team

  def isValid(text: String, context: BehaviorParameterContext) = {
    cachedValuesFor(context).map { cached =>
      Future.successful(cachedValidValueFor(text, context).isDefined)
    }.getOrElse {
      fetchMatchFor(text, context).map { maybeMatch =>
        maybeMatch.isDefined
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
      value <- values.find((ea) => textMatchesLabel(text, ea.label))
    } yield value
  }

  private def cachedValidValueFor(text: String, context: BehaviorParameterContext): Option[ValidValue] = {
    cachedValidValueAtIndex(text, context).
      orElse(cachedValidValueForLabel(text, context))
  }

  def prepareForInvocation(text: String, context: BehaviorParameterContext) = {
    val eventualMaybeMatch = cachedValidValueFor(text, context).map { validValue =>
      Future.successful(Some(validValue))
    }.getOrElse {
      fetchMatchFor(text, context)
    }

    eventualMaybeMatch.map { maybeMatch =>
      maybeMatch.
        map { v => JsObject(Map("id" -> JsString(v.id), "label" -> JsString(v.label))) }.
        getOrElse(JsString(text))
    }
  }

  val invalidPromptModifier: String = s"I need a $name. Use one of the numbers or labels below"

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
      maybeBehaviorVersion <- context.dataService.behaviors.maybeCurrentVersionFor(behavior)
      maybeResult <- maybeBehaviorVersion.map { behaviorVersion =>
        context.dataService.behaviorVersions.resultFor(behaviorVersion, paramsWithValues, context.event).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeResult
  }

  private def extractValidValues(result: SuccessResult): Seq[ValidValue] = {
    result.result.as[Seq[JsObject]].flatMap { ea =>
      ea.validate[ValidValue] match {
        case JsSuccess(data, jsPath) => Some(data)
        case e: JsError => None
      }
    }
  }

  private def fetchValidValues(context: BehaviorParameterContext): Future[Seq[ValidValue]] = {
    fetchValidValuesResult(None, context).map { maybeResult =>
      maybeResult.map {
        case r: SuccessResult => extractValidValues(r)
        case r: BotResult => Seq()
      }.getOrElse(Seq())
    }
  }

  private def textMatchesLabel(text: String, label: String): Boolean = {
    val lowercaseText = text.toLowerCase
    val insideOfFormattedLink = text.replaceFirst("""^<.+\|(.+)>$""", "$1").toLowerCase
    val lowercaseLabel = label.toLowerCase
    lowercaseLabel == lowercaseText || lowercaseLabel == insideOfFormattedLink
  }

  private def fetchMatchFor(text: String, context: BehaviorParameterContext): Future[Option[ValidValue]] = {
    fetchValidValues(context).map { validValues =>
      validValues.find {
        v => v.id == text || textMatchesLabel(text, v.label)
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
                                    maybePreviousCollectedValue: Option[CollectedParameterValue],
                                    context: BehaviorParameterContext
                                  ): Future[String] = {
    for {
      superPrompt <- super.promptFor(maybePreviousCollectedValue, context)
      maybeValidValuesResult <- fetchValidValuesResult(maybeSearchQuery, context)
      output <- maybeValidValuesResult.map {
        case r: SuccessResult => {
          val validValues = extractValidValues(r)
          if (validValues.isEmpty) {
            cancelAndRespondFor(s"This data type isn't returning any values: ${editLinkFor(context)}", context)
          } else {
            context.maybeConversation.foreach { conversation =>
              context.cache.set(valuesListCacheKeyFor(conversation, context.parameter), validValues, 5.minutes)
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
                                   maybePreviousCollectedValue: Option[CollectedParameterValue],
                                   context: BehaviorParameterContext
                                 ): Future[String] = {

    maybeCachedSearchQueryFor(context).map { searchQuery =>
      promptForListAllCase(Some(searchQuery), maybePreviousCollectedValue, context)
    }.getOrElse {
      super.promptFor(maybePreviousCollectedValue, context).map { superPrompt =>
        superPrompt ++ " Enter a search query:"
      }
    }
  }

  override def promptFor(
                           maybePreviousCollectedValue: Option[CollectedParameterValue],
                           context: BehaviorParameterContext
                         ): Future[String] = {
    context.dataService.behaviors.hasSearchParam(this.behavior).flatMap { usesSearch =>
      if (usesSearch) {
        promptForSearchCase(maybePreviousCollectedValue, context)
      } else {
        promptForListAllCase(None, maybePreviousCollectedValue, context)
      }
    }
  }

  override def handleCollected(event: MessageEvent, context: BehaviorParameterContext): Future[Unit] = {
    context.dataService.behaviors.hasSearchParam(this.behavior).flatMap { usesSearch =>
      if (usesSearch && maybeCachedSearchQueryFor(context).isEmpty && context.maybeConversation.isDefined) {
        val key = searchQueryCacheKeyFor(context.maybeConversation.get, context.parameter)
        val searchQuery = event.context.relevantMessageText
        context.cache.set(key, searchQuery, 5.minutes)
        Future.successful({})
      } else {
        super.handleCollected(event, context)
      }
    }
  }

}

object BehaviorParameterType {

  val allBuiltin = Seq(
    TextType,
    NumberType
  )

  def findBuiltIn(id: String): Option[BehaviorParameterType] = allBuiltin.find(_.id == id)

  def allFor(team: Team, dataService: DataService): Future[Seq[BehaviorParameterType]] = {
    dataService.behaviors.dataTypesForTeam(team).map { behaviorBacked =>
      allBuiltin ++ behaviorBacked.map(BehaviorBackedDataType.apply)
    }
  }

  def find(id: String, team: Team, dataService: DataService): Future[Option[BehaviorParameterType]] = {
    allFor(team, dataService).map { all =>
      all.find {
        case paramType: BehaviorBackedDataType => paramType.id == id || paramType.behavior.maybeImportedId.contains(id)
        case paramType: BehaviorParameterType => paramType.id == id
      }
    }
  }

}
