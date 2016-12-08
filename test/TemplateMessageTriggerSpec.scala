import models.IDs
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.triggers.TemplateMessageTrigger
import models.team.Team
import org.joda.time.LocalDateTime

class TemplateMessageTriggerSpec extends MessageTriggerSpec {

  def triggerFor(template: String, requiresBotMention: Boolean = false, isCaseSensitive: Boolean = false): TemplateMessageTrigger = {
    val team = Team(IDs.next, "Team!")
    val versionId = IDs.next
    val group = BehaviorGroup(IDs.next, "", None, None, team, LocalDateTime.now)
    val behavior = Behavior(IDs.next, team, Some(group), Some(versionId), None, None, LocalDateTime.now)
    val behaviorVersion = BehaviorVersion(versionId, behavior, None, None, None, None, forcePrivateResponse = false, None, LocalDateTime.now)
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
      val trigger1 = triggerFor("how much is ${amount} CAD in USD?")
      val trigger2 = triggerFor("w(.*)t+f")
      matches(trigger1, "how much is $100 CAD in USD?") mustBe true
      matches(trigger1, "how much is $100 CAD in US") mustBe false
      matches(trigger2, "w(.*)t+f") mustBe true
      matches(trigger2, "whattf") mustBe false
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
      val params = Seq(newParameterFor("version", 1, trigger.behaviorVersion))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        newParameterFor("version", 1, trigger.behaviorVersion),
        newParameterFor("subversion", 2, trigger.behaviorVersion)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

    "build two invocation parameters in different order" in {
      val trigger = triggerFor("deploy {version} {subversion}")
      val params = Seq(
        newParameterFor("subversion", 1, trigger.behaviorVersion),
        newParameterFor("version", 2, trigger.behaviorVersion)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "0.0.1", "param1" -> "ellipsis-12345")
    }

    "build a multi-token invocation parameter" in {
      val trigger = triggerFor("Where is {city}?")
      val params = Seq(newParameterFor("city", 1, trigger.behaviorVersion))
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
