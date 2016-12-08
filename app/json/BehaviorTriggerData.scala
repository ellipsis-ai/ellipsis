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

  private def containsNonRegexParam = {
    """\{.+\}""".r.findFirstMatchIn(this.text).isDefined
  }

  private def sortString: String = {
    if (this.isRegex) {
      "3 " ++ this.text
    } else if (this.beginsWithAlphanumeric) {
      if (this.containsNonRegexParam) {
        "1 " ++ this.text
      } else {
        "0 " ++ this.text
      }
    } else {
      "2 " ++ this.text
    }
  }

  def compare(that: BehaviorTriggerData): Int = {
    this.sortString compare that.sortString
  }
}
