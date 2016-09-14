package models.bots.triggers

import models.bots.behaviorparameter.BehaviorParameter
import models.bots.behaviorversion.BehaviorVersion

import scala.util.matching.Regex

case class RegexMessageTrigger(
                                id: String,
                                behaviorVersion: BehaviorVersion,
                                pattern: String,
                                requiresBotMention: Boolean,
                                isCaseSensitive: Boolean
                                ) extends MessageTrigger {

  def regex: Regex = pattern.r

  val shouldTreatAsRegex: Boolean = true

  val sortRank: Int = 2

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    0.to(regex.pattern.matcher("").groupCount()).map { i =>
      Some(i)
    }
  }
}
