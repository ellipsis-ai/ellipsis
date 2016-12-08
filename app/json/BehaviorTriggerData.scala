package json

import models.behaviors.triggers.messagetrigger.MessageTrigger

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean
                                ) extends Ordered[BehaviorTriggerData] {

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorTriggerData): Int = {
    MessageTrigger.sortKeyFor(this.text, this.isRegex) compare MessageTrigger.sortKeyFor(that.text, that.isRegex)
  }
}
