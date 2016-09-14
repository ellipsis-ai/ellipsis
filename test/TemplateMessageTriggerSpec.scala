import models.IDs
import models.bots.behavior.Behavior
import models.bots.behaviorparameter.BehaviorParameter
import models.bots.behaviorversion.BehaviorVersion
import models.bots.triggers.TemplateMessageTrigger
import models.team.Team
import org.joda.time.DateTime

class TemplateMessageTriggerSpec extends MessageTriggerSpec {

  def triggerFor(template: String, requiresBotMention: Boolean = false, isCaseSensitive: Boolean = false): TemplateMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val versionId = IDs.next
    val behavior = Behavior(IDs.next, team, Some(versionId), None, DateTime.now)
    val behaviorVersion = BehaviorVersion(versionId, behavior, None, None, None, None, None, DateTime.now)
    TemplateMessageTrigger(IDs.next, behaviorVersion, template, requiresBotMention, isCaseSensitive)
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
      matches(trigger, "how much is $100 CAD in US") mustBe false
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
      val params = Seq(BehaviorParameter(IDs.next, "version", 1, trigger.behaviorVersion, None, None))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "version", 1, trigger.behaviorVersion, None, None),
        BehaviorParameter(IDs.next, "subversion", 2, trigger.behaviorVersion, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

    "build two invocation parameters in different order" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        BehaviorParameter(IDs.next, "subversion", 1, trigger.behaviorVersion, None, None),
        BehaviorParameter(IDs.next, "version", 2, trigger.behaviorVersion, None, None)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "0.0.1", "param1" -> "ellipsis-12345")
    }

    "build a multi-token invocation parameter" in {
      val trigger = triggerFor("Where is {city}?")
      val params = Seq(BehaviorParameter(IDs.next, "city", 1, trigger.behaviorVersion, None, None))
      val invocationParams = trigger.invocationParamsFor("Where is San Francisco?", params)
      invocationParams mustBe Map("param0" -> "San Francisco")
    }

    "be permissive with curly single quotes in messages" in {
      val trigger = triggerFor("Single quotes are 'cool'")
      matches(trigger, "Single quotes are ‘cool’") mustBe true
    }

    "be permissive with curly single quotes in triggers" in {
      val trigger = triggerFor("Single quotes are ‘cool’")
      matches(trigger, "Single quotes are 'cool'") mustBe true
    }

    "be permissive with curly double quotes in messages" in {
      val trigger = triggerFor("Double quotes are \"cool\"")
      matches(trigger, "Double quotes are “cool”") mustBe true
    }

    "be permissive with curly double quotes in triggers" in {
      val trigger = triggerFor("Double quotes are “cool”")
      matches(trigger, "Double quotes are \"cool\"") mustBe true
    }

  }

}
