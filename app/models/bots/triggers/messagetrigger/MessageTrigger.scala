package models.bots.triggers.messagetrigger

import java.util.regex.PatternSyntaxException

import models.bots.behaviorparameter.BehaviorParameter
import models.bots.events.{Event, MessageEvent}
import models.bots.triggers.Trigger
import services.AWSLambdaConstants

import scala.util.matching.Regex

trait MessageTrigger extends Trigger {

  val pattern: String
  def regex: Regex
  val requiresBotMention: Boolean
  val shouldTreatAsRegex: Boolean
  val isCaseSensitive: Boolean
  val sortRank: Int

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
      case e: MessageEvent => invocationParamsFor(e.context.relevantMessageText, params)
      case _ => Map()
    }
  }

  def matches(relevantMessageText: String, includesBotMention: Boolean): Boolean = {
    isValidRegex && regex.findFirstMatchIn(relevantMessageText).nonEmpty && (!requiresBotMention || includesBotMention)
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: MessageEvent => matches(e.context.relevantMessageText, e.context.includesBotMention)
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

}
