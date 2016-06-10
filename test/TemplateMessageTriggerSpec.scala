import models.{IDs, Team}
import models.bots.{BehaviorParameter, Behavior}
import models.bots.triggers.TemplateMessageTrigger
import org.joda.time.DateTime

class TemplateMessageTriggerSpec extends MessageTriggerSpec {

  def triggerFor(template: String, requiresBotMention: Boolean = false, isCaseSensitive: Boolean = false): TemplateMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val behavior = Behavior(IDs.next, team, None, None, None, None, DateTime.now)
    TemplateMessageTrigger(IDs.next, behavior, template, requiresBotMention, isCaseSensitive)
  }

  "TemplateMessageTrigger" should {

    "be activated with one word param" in  {
      val trigger = triggerFor("deploy {version}")
      matches(trigger, "deploy foo") mustBe true
    }

    "be activated with two word param" in  {
      val trigger = triggerFor("deploy {version}")
      matches(trigger, "deploy foo bar") mustBe true
    }

    "be activated with two params" in  {
      val trigger = triggerFor("deploy {version} {subversion}")
      matches(trigger, "deploy foo bar") mustBe true
    }

    "be activated regardless of case" in {
      val trigger = triggerFor("deploy {version}")
      matches(trigger, "DEPLOY foo") mustBe true
    }

    "handle regex chars" in {
      val trigger = triggerFor("how much is ${amount} CAD in USD?")
      matches(trigger, "how much is $100 CAD in USD?") mustBe true
    }

    "not be activated without a required param" in {
      val trigger = triggerFor("deploy {version}")
      matches(trigger, "deploy") mustBe false
    }

    "not be activated when not at beginning of text" in {
      val trigger = triggerFor("deploy {version}")
      matches(trigger, "yo deploy") mustBe false
    }

    "not be activated when missing required bot mention" in {
      val trigger = triggerFor("deploy {version}", requiresBotMention = true)
      matches(trigger, "deploy foo", includesBotMention = false) mustBe false
    }

    "be activated when includes required bot mention" in {
      val trigger = triggerFor("deploy {version}", requiresBotMention = true)
      matches(trigger, "deploy foo", includesBotMention = true) mustBe true
    }

    "build an invocation parameter" in {
      val trigger = triggerFor("deploy {version}")
      val params = Seq(BehaviorParameter(IDs.next, "version", 1, trigger.behavior, None, None))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "version", 1, trigger.behavior, None, None),
        BehaviorParameter(IDs.next, "subversion", 2, trigger.behavior, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

    "build two invocation parameters in different order" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "subversion", 1, trigger.behavior, None, None),
        BehaviorParameter(IDs.next, "version", 2, trigger.behavior, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "0.0.1", "param1" -> "ellipsis-12345")
    }

  }

}
