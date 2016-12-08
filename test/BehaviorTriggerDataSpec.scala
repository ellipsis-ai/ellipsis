import org.scalatestplus.play.PlaySpec
import json.BehaviorTriggerData

class BehaviorTriggerDataSpec extends PlaySpec {

  def triggerFor(string: String, isRegex: Boolean): BehaviorTriggerData = {
    BehaviorTriggerData(string, requiresMention = false, isRegex, caseSensitive = false)
  }

  "BehaviorTriggerData" should {
    "sort non-regex triggers before regex triggers" in {
      val trigger1 = triggerFor("a", isRegex = true)
      val trigger2 = triggerFor("b", isRegex = false)
      Array(trigger1, trigger2).sorted mustBe Array(trigger2, trigger1)
    }

    "sort non-regex triggers that start with an alphanumeric before non-regex triggers that don't" in {
      val trigger1 = triggerFor(":tada:", isRegex = false)
      val trigger2 = triggerFor("lalala", isRegex = false)
      val trigger3 = triggerFor("a is for apple", isRegex = false)
      Array(trigger1, trigger2, trigger3).sorted mustBe Array(trigger3, trigger2, trigger1)
    }
  }
}
