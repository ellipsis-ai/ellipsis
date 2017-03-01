package json

import models.behaviors.triggers.messagetrigger.MessageTrigger
import utils.FuzzyMatchable

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean
                                ) extends Ordered[BehaviorTriggerData] with FuzzyMatchable {

  val maybeText: Option[String] = Some(text)

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorTriggerData): Int = {
    MessageTrigger.sortKeyFor(this.text, this.isRegex) compare MessageTrigger.sortKeyFor(that.text, that.isRegex)
  }

  val maybeFuzzyMatchPattern: Option[String] = {
    if (isRegex) {
      None
    } else {
      Some(text)
    }
  }
}
