package json

case class BehaviorTriggerData(
                                text: String,
                                requiresMention: Boolean,
                                isRegex: Boolean,
                                caseSensitive: Boolean
                                ) extends Ordered[BehaviorTriggerData] {

  def sortString: String = {
    if ("""^[A-Za-z0-9]""".r.findFirstMatchIn(this.text).isDefined) {
      this.text
    } else {
      "~" ++ this.text
    }
  }

  def compare(that: BehaviorTriggerData): Int = {
    if (this.isRegex && !that.isRegex) {
      1
    } else if (!this.isRegex && that.isRegex) {
      -1
    } else if (!this.isRegex && !that.isRegex) {
      this.sortString compare that.sortString
    } else {
      this.text compare that.text
    }
  }
}
