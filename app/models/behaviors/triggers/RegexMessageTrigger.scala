package models.behaviors.triggers

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.triggers.messagetrigger.MessageTrigger

import scala.util.matching.Regex

case class RegexMessageTrigger(
                                id: String,
                                behaviorVersion: BehaviorVersion,
                                pattern: String,
                                requiresBotMention: Boolean,
                                isCaseSensitive: Boolean
                                ) extends MessageTrigger {

  def regex: Regex = {
    if (isCaseSensitive) {
      pattern.r
    } else {
      s"(?i)${pattern}".r
    }
  }

  val shouldTreatAsRegex: Boolean = true

  val sortRank: Int = 2

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    0.to(regex.pattern.matcher("").groupCount()).map { i =>
      Some(i)
    }
  }
}
