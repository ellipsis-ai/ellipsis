package json

import models.behaviors.triggers.{MessageSent, Trigger}
import utils.FuzzyMatchPattern

// The existing structure of this class should be maintained and
// new fields should be optional for future compatibility
case class LegacyBehaviorTriggerJson(
                                      text: String,
                                      requiresMention: Boolean,
                                      isRegex: Boolean,
                                      caseSensitive: Boolean,
                                      triggerType: Option[String]
                                    ) {
  def toBehaviorTriggerData: BehaviorTriggerData = {
    BehaviorTriggerData(text, requiresMention, isRegex, caseSensitive, triggerType.getOrElse(MessageSent.toString))
  }
}

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean,
                                triggerType: String
                              ) extends Ordered[BehaviorTriggerData] with FuzzyMatchPattern {

  val maybeText: Option[String] = Some(text)

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorTriggerData): Int = {
    Trigger.sortKeyFor(this.text, this.isRegex) compare Trigger.sortKeyFor(that.text, that.isRegex)
  }

  val maybePattern: Option[String] = {
    if (isRegex) {
      None
    } else {
      Some(text)
    }
  }
}

object BehaviorTriggerData {
  def fromTrigger(trigger: Trigger): BehaviorTriggerData = {
    BehaviorTriggerData(
      trigger.pattern,
      requiresMention = trigger.requiresBotMention,
      isRegex = trigger.shouldTreatAsRegex,
      caseSensitive = trigger.isCaseSensitive,
      triggerType = trigger.triggerType.toString
    )
  }
}
