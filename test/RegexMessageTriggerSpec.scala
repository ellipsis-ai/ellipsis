import models.{IDs, Team}
import models.bots.{BehaviorParameter, Behavior}
import models.bots.triggers.RegexMessageTrigger
import org.joda.time.DateTime

class RegexMessageTriggerSpec extends MessageTriggerSpec {

  def triggerFor(pattern: String, requiresBotMention: Boolean = false, isCaseSensitive: Boolean = false): RegexMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val behavior = Behavior(IDs.next, team, None, None, None, None, DateTime.now)
    RegexMessageTrigger(IDs.next, behavior, pattern.r, requiresBotMention, isCaseSensitive)
  }

  val oneParamPattern = """deploy\s+(\S+)"""
  val twoParamPattern = """deploy\s+(\S+)\s+(\S+)"""

  "RegexMessageTrigger" should {

    "be activated with one word param" in  {
      val trigger = triggerFor(oneParamPattern)
      matches(trigger, "deploy foo") mustBe true
    }

    "be activated with two word param" in  {
      val trigger = triggerFor(oneParamPattern)
      matches(trigger, "deploy foo bar") mustBe true
    }

    "be activated with two params" in  {
      val trigger = triggerFor(twoParamPattern)
      matches(trigger, "deploy foo bar") mustBe true
    }

    "not be activated without a required param" in {
      val trigger = triggerFor(oneParamPattern)
      matches(trigger, "deploy") mustBe false
    }

    "not be activated when missing required bot mention" in {
      val trigger = triggerFor(oneParamPattern, requiresBotMention = true)
      matches(trigger, "deploy foo", includesBotMention = false) mustBe false
    }

    "be activated when includes required bot mention" in {
      val trigger = triggerFor(oneParamPattern, requiresBotMention = true)
      matches(trigger, "deploy foo", includesBotMention = true) mustBe true
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
