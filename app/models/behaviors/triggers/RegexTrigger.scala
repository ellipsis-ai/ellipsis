package models.behaviors.triggers

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion

import scala.util.matching.Regex

case class RegexTrigger(
                                id: String,
                                behaviorVersion: BehaviorVersion,
                                triggerType: TriggerType,
                                pattern: String,
                                requiresBotMention: Boolean,
                                isCaseSensitive: Boolean
                                ) extends Trigger {

  def regex: Regex = {
    if (isCaseSensitive) {
      pattern.r
    } else {
      s"(?i)${pattern}".r
    }
  }

  val shouldTreatAsRegex: Boolean = true

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    0.to(regex.pattern.matcher("").groupCount()).map { i =>
      Some(i)
    }
  }
}
