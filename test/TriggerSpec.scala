import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.triggers.Trigger
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

trait TriggerSpec extends PlaySpec with MockitoSugar {

  def matches(trigger: Trigger, text: String, includesBotMention: Boolean = false): Boolean = {
    trigger.matches(text, includesBotMention)
  }

  def newParameterFor(
                    name: String,
                    rank: Int,
                    behaviorVersion: BehaviorVersion
                  ): BehaviorParameter = {
    val groupVersion = mock[BehaviorGroupVersion]
    BehaviorParameter(IDs.next, rank, Input(IDs.next, IDs.next, None, name, None, TextType, false, false, groupVersion), behaviorVersion)
  }

}

class TriggerObjectSpec extends PlaySpec {
  "sortKeyFor" should {
    "sort non-regex triggers before regex triggers" in {
      val trigger1 = Trigger.sortKeyFor("a", isRegex = true)
      val trigger2 = Trigger.sortKeyFor("b", isRegex = false)
      Array(trigger1, trigger2).sorted mustBe Array(trigger2, trigger1)
    }

    "sort non-regex triggers that start with an alphanumeric before non-regex triggers that don't" in {
      val trigger1 = Trigger.sortKeyFor(":tada:", isRegex = false)
      val trigger2 = Trigger.sortKeyFor("lalala", isRegex = false)
      val trigger3 = Trigger.sortKeyFor("a is for apple", isRegex = false)
      Array(trigger1, trigger2, trigger3).sorted mustBe Array(trigger3, trigger2, trigger1)
    }

    "sort non-regex triggers with params after non-regex triggers without params" in {
      val trigger1 = Trigger.sortKeyFor("a {fruit}", isRegex = false)
      val trigger2 = Trigger.sortKeyFor("be fruit", isRegex = false)
      Array(trigger1, trigger2).sorted mustBe Array(trigger2, trigger1)
    }

    "sort all permutations of triggers correctly" in {
      val regex1 = Trigger.sortKeyFor("abc", isRegex = true)
      val regex2 = Trigger.sortKeyFor(".+", isRegex = true)
      val nonRegexNonAlphanumeric = Trigger.sortKeyFor(":tada:", isRegex = false)
      val nonRegexParams = Trigger.sortKeyFor("yyy {zzz}", isRegex = false)
      val nonRegexNoParams = Trigger.sortKeyFor("zzzz", isRegex = false)
      Array(regex1, regex2, nonRegexNonAlphanumeric, nonRegexParams, nonRegexNoParams).sorted mustBe {
        Array(nonRegexNoParams, nonRegexParams, nonRegexNonAlphanumeric, regex2, regex1)
      }
    }
  }
}
