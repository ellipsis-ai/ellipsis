package models.bots.triggers

import models.bots.{SlackMessageEvent, Event}
import services.AWSLambdaConstants

import scala.util.matching.Regex

trait MessageTrigger extends Trigger {

  val regex: Regex

  def paramsFor(event: Event): Map[String, String] = {
    event match {
      case e: SlackMessageEvent => {
        regex.findFirstMatchIn(e.context.message.text).map { firstMatch =>
          firstMatch.subgroups.zipWithIndex.map { case(param, i) =>
            (AWSLambdaConstants.invocationParamFor(i), param)
          }.toMap
        }.getOrElse(Map())
      }
      case _ => Map()
    }
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: SlackMessageEvent => regex.findFirstMatchIn(e.context.message.text).nonEmpty
      case _ => false
    }
  }

}
