package models.behaviors.triggers

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric
import models.behaviors.triggers.messagetrigger.MessageTrigger

case class TriggerFuzzyMatcher(matchString: String, triggers: Seq[MessageTrigger], threshold: Double = 0.6) {

  val matchTokenCount: Int = matchString.split("\\s+").length

  def ngramsFor(pattern: String): Seq[String] = {
    pattern.split("\\s+").sliding(matchTokenCount, 1).map(tokens => tokens.mkString(" ")).toSeq
  }

  def basicScoreFor(text: String): Double = RatcliffObershelpMetric.compare(text, matchString).getOrElse(0)

  def scoreFor(trigger: MessageTrigger): Double = {
    trigger.maybeFuzzyMatchPattern.map { pattern =>
      ngramsFor(pattern).map(basicScoreFor).max
    }.getOrElse(0)
  }

  def run: Seq[(MessageTrigger, Double)] = {
    triggers.map { ea =>
      (ea, scoreFor(ea))
    }.
      filter { case(_, similarity) => similarity > threshold }.
      sortBy { case(_, similarity) => similarity }.
      reverse
  }

}
