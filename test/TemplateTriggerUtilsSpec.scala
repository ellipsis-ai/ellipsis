import models.behaviors.triggers.TemplateTriggerUtils
import org.scalatestplus.play.PlaySpec

class TemplateTriggerUtilsSpec extends PlaySpec {


  "TemplateTriggerUtils" should {

    "escape regex chars" in  {
      val pattern = "how much is $100 CAD in USD?"
      TemplateTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """how much is \$100 CAD in USD\?"""
    }

    "don't replace { } used for params" in  {
      val pattern = "how much is ${amount} CAD in USD?"
      TemplateTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """how much is \${amount} CAD in USD\?"""
    }

    "work with strings containing \\" in  {
      val pattern = "Need to deal with \\ correctly"
      TemplateTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """Need to deal with \\ correctly"""
    }
  }

}
