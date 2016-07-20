import models.bots.triggers.TemplateMessageTriggerUtils
import org.scalatestplus.play.PlaySpec

class TemplateMessageTriggerUtilsSpec extends PlaySpec {


  "TemplateMessageTriggerUtils" should {

    "escape regex chars" in  {
      val pattern = "how much is $100 CAD in USD?"
      TemplateMessageTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """how much is \$100 CAD in USD\?"""
    }

    "don't replace { } used for params" in  {
      val pattern = "how much is ${amount} CAD in USD?"
      TemplateMessageTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """how much is \${amount} CAD in USD\?"""
    }

    "work with strings containing \\" in  {
      val pattern = "Need to deal with \\ correctly"
      TemplateMessageTriggerUtils.escapeRegexCharactersIn(pattern) mustBe """Need to deal with \\ correctly"""
    }
  }

}
