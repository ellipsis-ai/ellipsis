package models.behaviors.behaviorparameter

import models.behaviors.{BotResult, SuccessResult}
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.team.Team
import play.api.libs.json._
import services.DataService

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait BehaviorParameterType {

  val id: String
  val name: String

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

}

trait BuiltInType extends BehaviorParameterType {
  lazy val id = name
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


case class BehaviorBackedDataType(id: String, name: String, behavior: Behavior) extends BehaviorParameterType {

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
      values <- context.cache.get[Seq[ValidValue]](cacheKeyFor(conversation, context.parameter))
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
      value <- values.find(_.label.toLowerCase == text.toLowerCase)
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

  private def cacheKeyFor(conversation: Conversation, parameter: BehaviorParameter): String = s"${conversation.id}-${parameter.id}"

  private def fetchValidValuesResult(context: BehaviorParameterContext): Future[Option[BotResult]] = {
    for {
      maybeBehaviorVersion <- context.dataService.behaviors.maybeCurrentVersionFor(behavior)
      maybeResult <- maybeBehaviorVersion.map { behaviorVersion =>
        context.dataService.behaviorVersions.resultFor(behaviorVersion, Seq(), context.event).map(Some(_))
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
    fetchValidValuesResult(context).map { maybeResult =>
      maybeResult.map {
        case r: SuccessResult => extractValidValues(r)
        case r: BotResult => Seq()
      }.getOrElse(Seq())
    }
  }

  private def fetchMatchFor(text: String, context: BehaviorParameterContext): Future[Option[ValidValue]] = {
    fetchValidValues(context).map { validValues =>
      validValues.find { v => v.id == text || v.label.toLowerCase == text.toLowerCase }
    }
  }

  private def cancelAndRespondFor(response: String, context: BehaviorParameterContext): Future[String] = {
    context.dataService.conversations.cancel(context.maybeConversation).map { _ =>
      s"$response\n\n(Your current action has been cancelled. Try again after fixing the problem.)"
    }
  }

  override def promptFor(
                          maybePreviousCollectedValue: Option[CollectedParameterValue],
                          context: BehaviorParameterContext
                        ): Future[String] = {
    for {
      superPrompt <- super.promptFor(maybePreviousCollectedValue, context)
      maybeValidValuesResult <- fetchValidValuesResult(context)
      output <- maybeValidValuesResult.map {
        case r: SuccessResult => {
          val validValues = extractValidValues(r)
          context.maybeConversation.foreach { conversation =>
            context.cache.set(cacheKeyFor(conversation, context.parameter), validValues, 5.minutes)
          }
          val valuesPrompt = validValues.zipWithIndex.map { case(ea, i) =>
            s"\n\n$i. ${ea.label}"
          }.mkString
          Future.successful(superPrompt ++ valuesPrompt)
        }
        case r: BotResult => cancelAndRespondFor(r.fullText, context)
      }.getOrElse {
        cancelAndRespondFor(s"This data type appears to be misconfigured: ${context.parameter.paramType.name}", context)
      }
    } yield output

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
