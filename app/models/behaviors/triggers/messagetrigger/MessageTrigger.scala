package models.behaviors.triggers.messagetrigger

import java.util.regex.PatternSyntaxException

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.events.{Event, MessageEvent}
import models.behaviors.triggers.Trigger
import services.AWSLambdaConstants
import utils.FuzzyMatchPattern

import scala.util.matching.Regex

trait MessageTrigger extends Trigger with FuzzyMatchPattern {

  val pattern: String
  val maybePattern: Option[String] = None
  def regex: Regex
  val requiresBotMention: Boolean
  val shouldTreatAsRegex: Boolean
  val isCaseSensitive: Boolean

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]]

  def isValidRegex: Boolean = {
    try {
      regex
      true
    } catch {
      case e: PatternSyntaxException => false
    }
  }

  def invocationParamsFor(message: String, params: Seq[BehaviorParameter]): Map[String, String] = {
    regex.findFirstMatchIn(message).map { firstMatch =>
      firstMatch.subgroups.zip(paramIndexMaybesFor(params)).flatMap { case(paramValue, maybeRank) =>
        maybeRank.map { rank =>
          (AWSLambdaConstants.invocationParamFor(rank), paramValue)
        }
      }.toMap
    }.getOrElse(Map())
  }

  def invocationParamsFor(event: Event, params: Seq[BehaviorParameter]): Map[String, String] = {
    event match {
      case e: MessageEvent => invocationParamsFor(e.relevantMessageText, params)
      case _ => Map()
    }
  }

  def matches(relevantMessageText: String, includesBotMention: Boolean): Boolean = {
    isValidRegex && regex.findFirstMatchIn(relevantMessageText).nonEmpty && (!requiresBotMention || includesBotMention)
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: MessageEvent => matches(e.relevantMessageText, e.includesBotMention)
      case _ => false
    }

  }

}

object MessageTrigger {

  def maybeRegexValidationErrorFor(pattern: String): Option[String] = {
    try {
      pattern.r
      None
    } catch {
      case e: PatternSyntaxException => Some(e.getMessage)
    }
  }

  private def textBeginsWithAlphanumeric(text: String) = {
    """^[A-Za-z0-9]""".r.findFirstMatchIn(text).isDefined
  }

  private def textContainsTemplateParam(text: String) = {
    """\{.+\}""".r.findFirstMatchIn(text).isDefined
  }

  def sortKeyFor(text: String, isRegex: Boolean): (Int, String) = {
    if (isRegex) {
      (3, text)
    } else if (!textBeginsWithAlphanumeric(text)) {
      (2, text)
    } else if (textContainsTemplateParam(text)) {
      (1, text)
    } else {
      (0, text)
    }
  }
}
