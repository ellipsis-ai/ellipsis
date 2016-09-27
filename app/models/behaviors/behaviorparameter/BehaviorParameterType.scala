package models.behaviors.behaviorparameter

import models.behaviors.SuccessResult
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.team.Team
import play.api.cache.CacheApi
import play.api.libs.json._
import services.DataService

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait BehaviorParameterType {

  val id: String
  val name: String

  def isValid(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi): Future[Boolean]

  def prepareForInvocation(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi): Future[JsValue]

  def invalidPromptModifier: String

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[CollectedParameterValue]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      s" (${invalidPromptModifier})"
    } else {
      ""
    }
  }

  def promptFor(
                 parameter: BehaviorParameter,
                 conversation: Conversation,
                 maybePreviousCollectedValue: Option[CollectedParameterValue],
                 event: MessageEvent,
                 dataService: DataService,
                 cache: CacheApi
               ): Future[String] = {
      Future.successful(s"${parameter.question}${invalidValueModifierFor(maybePreviousCollectedValue)}")
    }

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
}

object TextType extends BuiltInType {
  val name = "Text"

  def isValid(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = Future.successful(true)

  def prepareForInvocation(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = Future.successful(JsString(text))

  val invalidPromptModifier: String = "I need a valid answer"

}

object NumberType extends BuiltInType {
  val name = "Number"

  def isValid(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = Future.successful {
    try {
      text.toDouble
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareForInvocation(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = Future.successful {
    try {
      JsNumber(BigDecimal(text))
    } catch {
      case e: NumberFormatException => JsString(text)
    }
  }

  val invalidPromptModifier: String = "I need a number"
}


case class BehaviorBackedDataType(id: String, name: String, behavior: Behavior) extends BehaviorParameterType {

  case class ValidValue(id: String, label: String)
  implicit val validValueReads = Json.reads[ValidValue]

  def isValid(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = {
    cachedValuesFor(maybeConversation, parameter, cache).map { cached =>
      Future.successful(cachedValidValueFor(text, maybeConversation, parameter, cache).isDefined)
    }.getOrElse {
      // TODO: do a real check here
      Future.successful(false)
    }
  }

  private def cachedValuesFor(
                               maybeConversation: Option[Conversation],
                               parameter: BehaviorParameter,
                               cache: CacheApi
                             ): Option[Seq[ValidValue]] = {
    for {
      conversation <- maybeConversation
      values <- cache.get[Seq[ValidValue]](cacheKeyFor(conversation, parameter))
    } yield values
  }

  private def cachedValidValueAtIndex(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi): Option[ValidValue] = {
    for {
      values <- cachedValuesFor(maybeConversation, parameter, cache)
      value <- try {
        val index = text.toInt - 1
        Some(values(index))
      } catch {
        case e: NumberFormatException => None
        case e: IndexOutOfBoundsException => None
      }
    } yield value
  }

  private def cachedValidValueForLabel(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi): Option[ValidValue] = {
    for {
      values <- cachedValuesFor(maybeConversation, parameter, cache)
      value <- values.find(_.label == text)
    } yield value
  }

  private def cachedValidValueFor(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi): Option[ValidValue] = {
    cachedValidValueAtIndex(text, maybeConversation, parameter, cache).
      orElse(cachedValidValueForLabel(text, maybeConversation, parameter, cache))
  }

  def prepareForInvocation(text: String, maybeConversation: Option[Conversation], parameter: BehaviorParameter, cache: CacheApi) = {
    val value = cachedValidValueFor(text, maybeConversation, parameter, cache).map { validValue =>
     JsObject(Map("id" -> JsString(validValue.id), "label" -> JsString(validValue.label)))
    }.getOrElse(JsString(text))
    Future.successful(value)
  }

  val invalidPromptModifier: String = s"I need a $name. Use one of the numbers or labels below"

  private def cacheKeyFor(conversation: Conversation, parameter: BehaviorParameter): String = s"${conversation.id}-${parameter.id}"

  private def fetchValidValues(event: MessageEvent, dataService: DataService): Future[Seq[ValidValue]] = {
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      maybeResult <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorVersions.resultFor(behaviorVersion, Seq(), event).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      maybeResult.map {
        case r: SuccessResult => {
          r.result.as[Seq[JsObject]].flatMap { ea =>
            ea.validate[ValidValue] match {
              case JsSuccess(data, jsPath) => Some(data)
              case e: JsError => None
            }
          }
        }
        case _ => Seq()
      }.getOrElse(Seq())
    }
  }

  override def promptFor(
                          parameter: BehaviorParameter,
                          conversation: Conversation,
                          maybePreviousCollectedValue: Option[CollectedParameterValue],
                          event: MessageEvent,
                          dataService: DataService,
                          cache: CacheApi
                        ): Future[String] = {
    for {
      superPrompt <- super.promptFor(parameter, conversation, maybePreviousCollectedValue, event, dataService, cache)
      validValues <- fetchValidValues(event, dataService)
    } yield {
      cache.set(cacheKeyFor(conversation, parameter), validValues, 5.minutes)
      val valuesPrompt = validValues.zipWithIndex.map { case(ea, i) =>
        s"\n\n$i. ${ea.label}"
      }.mkString
      superPrompt ++ valuesPrompt
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
    dataService.behaviorBackedDataTypes.allFor(team).map { behaviorBacked =>
      allBuiltin ++ behaviorBacked
    }
  }

  def find(id: String, team: Team, dataService: DataService): Future[Option[BehaviorParameterType]] = {
    allFor(team, dataService).map { all => all.find(_.id == id) }
  }

}
