import models.behaviors.builtins.BuiltinBehavior._
import org.scalatestplus.play.PlaySpec

class BuiltinBehaviorSpec extends PlaySpec {

  "schedule and unschedule regex" should {
    "match backtick-quoted triggers" in {
      "schedule `go bananas` every day at 3pm" must fullyMatch regex(scheduleRegex withGroups(
        "`", "go bananas", null, "every day at 3pm"
      ))

      "unschedule `go bananas`" must fullyMatch regex(unscheduleRegex withGroups(
        "`", "go bananas"
      ))
    }

    "match double-quoted triggers" in {
      """schedule "go bananas" every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
        "\"", "go bananas", null, "every day at 3pm"
      ))

      """unschedule "go bananas"""" must fullyMatch regex(unscheduleRegex withGroups(
        "\"", "go bananas"
      ))
    }

    "match single-quoted triggers" in {
      "schedule 'go bananas' every day at 3pm" must fullyMatch regex(scheduleRegex withGroups(
        "'", "go bananas", null, "every day at 3pm"
      ))

      "unschedule 'go bananas'" must fullyMatch regex(unscheduleRegex withGroups(
        "'", "go bananas"
      ))
    }

    "match even when there are nested quotes" in {
      """schedule `"go bananas," he said` every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
        "`", """"go bananas," he said""", null, "every day at 3pm"
      ))

      """unschedule `"go bananas," he said`""" must fullyMatch regex(unscheduleRegex withGroups(
        "`", """"go bananas," he said"""
      ))

      """schedule "he said 'go bananas'" every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
        "\"", "he said 'go bananas'", null, "every day at 3pm"
      ))

      """unschedule "he said 'go bananas'"""" must fullyMatch regex(unscheduleRegex withGroups(
        "\"", "he said 'go bananas'"
      ))
    }

    "match privately for everyone in this channel" in {
      """schedule `go bananas` privately for everyone in this channel every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
        "`", "go bananas", " privately for everyone in this channel", "every day at 3pm"
      ))
    }
  }
}
