import models.{IDs, Team}
import models.bots.{BehaviorParameter, Behavior}
import models.bots.triggers.TemplateMessageTrigger
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

class TemplateMessageTriggerSpec extends PlaySpec {

  def triggerFor(template: String): TemplateMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val behavior = Behavior(IDs.next, team, None, None, None, None, DateTime.now)
    TemplateMessageTrigger(IDs.next, behavior, template)
  }

  "TemplateMessageTrigger" should {

    "be activated with one word param" in  {
      val trigger = triggerFor("deploy {version}")
      trigger.matches("deploy foo") mustBe true
    }

    "be activated with two word param" in  {
      val trigger = triggerFor("deploy {version}")
      trigger.matches("deploy foo bar") mustBe true
    }

    "be activated with two params" in  {
      val trigger = triggerFor("deploy {version} {subversion}")
      trigger.matches("deploy foo bar") mustBe true
    }

    "not be activated without a required param" in {
      val trigger = triggerFor("deploy {version}")
      trigger.matches("deploy") mustBe false
    }

    "not be activated when not at beginning of text" in {
      val trigger = triggerFor("deploy {version}")
      trigger.matches("yo deploy") mustBe false
    }

    "build an invocation parameter" in {
      val trigger = triggerFor("deploy {version}")
      val params = Seq(BehaviorParameter(IDs.next, "version", 0, trigger.behavior, None, None))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "version", 0, trigger.behavior, None, None),
        BehaviorParameter(IDs.next, "subversion", 1, trigger.behavior, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

    "build two invocation parameters in different order" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "subversion", 0, trigger.behavior, None, None),
        BehaviorParameter(IDs.next, "version", 1, trigger.behavior, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "0.0.1", "param1" -> "ellipsis-12345")
    }

  }

}
