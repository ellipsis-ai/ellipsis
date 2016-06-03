package models.bots.triggers

import models.bots.{BehaviorParameter, SlackMessageEvent, Event}
import services.AWSLambdaConstants

import scala.util.matching.Regex

trait MessageTrigger extends Trigger {

  val regex: Regex

  protected def rankMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]]

  def invocationParamsFor(message: String, params: Seq[BehaviorParameter]): Map[String, String] = {
    regex.findFirstMatchIn(message).map { firstMatch =>
      firstMatch.subgroups.zip(rankMaybesFor(params)).flatMap { case(paramValue, maybeRank) =>
        maybeRank.map { rank =>
          (AWSLambdaConstants.invocationParamFor(rank), paramValue)
        }
      }.toMap
    }.getOrElse(Map())
  }

  def invocationParamsFor(event: Event, params: Seq[BehaviorParameter]): Map[String, String] = {
    event match {
      case e: SlackMessageEvent => invocationParamsFor(e.context.message.text, params)
      case _ => Map()
    }
  }

  def matches(text: String): Boolean = regex.findFirstMatchIn(text).nonEmpty

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: SlackMessageEvent => matches(e.context.message.text)
      case _ => false
    }
  }

}
