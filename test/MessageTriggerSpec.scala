import models.IDs
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.scalatestplus.play.PlaySpec

trait MessageTriggerSpec extends PlaySpec {

  def matches(trigger: MessageTrigger, text: String, includesBotMention: Boolean = false): Boolean = {
    trigger.matches(text, includesBotMention)
  }

  def newParameterFor(
                    name: String,
                    rank: Int,
                    behaviorVersion: BehaviorVersion
                  ): BehaviorParameter = {
    BehaviorParameter(IDs.next, rank, Input(IDs.next, None, name, None, TextType, false, false, None), behaviorVersion)
  }

}

class MessageTriggerObjectSpec extends PlaySpec {
  "sortKeyFor" should {
    "sort non-regex triggers before regex triggers" in {
      val trigger1 = MessageTrigger.sortKeyFor("a", isRegex = true)
      val trigger2 = MessageTrigger.sortKeyFor("b", isRegex = false)
      Array(trigger1, trigger2).sorted mustBe Array(trigger2, trigger1)
    }

    "sort non-regex triggers that start with an alphanumeric before non-regex triggers that don't" in {
      val trigger1 = MessageTrigger.sortKeyFor(":tada:", isRegex = false)
      val trigger2 = MessageTrigger.sortKeyFor("lalala", isRegex = false)
      val trigger3 = MessageTrigger.sortKeyFor("a is for apple", isRegex = false)
      Array(trigger1, trigger2, trigger3).sorted mustBe Array(trigger3, trigger2, trigger1)
    }

    "sort non-regex triggers with params after non-regex triggers without params" in {
      val trigger1 = MessageTrigger.sortKeyFor("a {fruit}", isRegex = false)
      val trigger2 = MessageTrigger.sortKeyFor("be fruit", isRegex = false)
      Array(trigger1, trigger2).sorted mustBe Array(trigger2, trigger1)
    }

    "sort all permutations of triggers correctly" in {
      val regex1 = MessageTrigger.sortKeyFor("abc", isRegex = true)
      val regex2 = MessageTrigger.sortKeyFor(".+", isRegex = true)
      val nonRegexNonAlphanumeric = MessageTrigger.sortKeyFor(":tada:", isRegex = false)
      val nonRegexParams = MessageTrigger.sortKeyFor("yyy {zzz}", isRegex = false)
      val nonRegexNoParams = MessageTrigger.sortKeyFor("zzzz", isRegex = false)
      Array(regex1, regex2, nonRegexNonAlphanumeric, nonRegexParams, nonRegexNoParams).sorted mustBe {
        Array(nonRegexNoParams, nonRegexParams, nonRegexNonAlphanumeric, regex2, regex1)
      }
    }
  }
}
