package json

import models.behaviors.triggers.messagetrigger.MessageTrigger
import utils.FuzzyMatchPattern

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean
                                ) extends Ordered[BehaviorTriggerData] with FuzzyMatchPattern {

  val maybeText: Option[String] = Some(text)

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorTriggerData): Int = {
    MessageTrigger.sortKeyFor(this.text, this.isRegex) compare MessageTrigger.sortKeyFor(that.text, that.isRegex)
  }

  val maybePattern: Option[String] = {
    if (isRegex) {
      None
    } else {
      Some(text)
    }
  }
}
