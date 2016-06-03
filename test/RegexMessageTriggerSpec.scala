import models.{IDs, Team}
import models.bots.{BehaviorParameter, Behavior}
import models.bots.triggers.RegexMessageTrigger
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

class RegexMessageTriggerSpec extends PlaySpec {

  def triggerFor(pattern: String): RegexMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val behavior = Behavior(IDs.next, team, None, None, None, None, DateTime.now)
    RegexMessageTrigger(IDs.next, behavior, pattern.r)
  }

  val oneParamPattern = """deploy\s+(\S+)"""
  val twoParamPattern = """deploy\s+(\S+)\s+(\S+)"""

  "RegexMessageTrigger" should {

    "be activated with one word param" in  {
      val trigger = triggerFor(oneParamPattern)
      trigger.matches("deploy foo") mustBe true
    }

    "be activated with two word param" in  {
      val trigger = triggerFor(oneParamPattern)
      trigger.matches("deploy foo bar") mustBe true
    }

    "be activated with two params" in  {
      val trigger = triggerFor(twoParamPattern)
      trigger.matches("deploy foo bar") mustBe true
    }

    "not be activated without a required param" in {
      val trigger = triggerFor(oneParamPattern)
      trigger.matches("deploy") mustBe false
    }

    "build an invocation parameter" in {
      val trigger = triggerFor(oneParamPattern)
      val params = Seq(BehaviorParameter(IDs.next, "version", 1, trigger.behavior, None, None))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor(twoParamPattern)
      val params = Seq(
        BehaviorParameter(IDs.next, "version", 1, trigger.behavior, None, None),
        BehaviorParameter(IDs.next, "subversion", 2, trigger.behavior, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

  }

}
