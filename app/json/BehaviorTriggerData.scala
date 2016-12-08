package json

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean
                                ) extends Ordered[BehaviorTriggerData] {

  private def beginsWithAlphanumeric = {
    """^[A-Za-z0-9]""".r.findFirstMatchIn(this.text).isDefined
  }

  private def containsTemplateParam = {
    """\{.+\}""".r.findFirstMatchIn(this.text).isDefined
  }

  private def sortKey: (Int, String) = {
    if (this.isRegex) {
      (3, this.text)
    } else if (!this.beginsWithAlphanumeric) {
      (2, this.text)
    } else if (this.containsTemplateParam) {
      (1, this.text)
    } else {
      (0, this.text)
    }
  }

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorTriggerData): Int = {
    this.sortKey compare that.sortKey
  }
}
