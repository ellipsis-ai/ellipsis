import java.time.OffsetDateTime

import models.IDs
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.triggers.RegexMessageTrigger
import models.team.Team

class RegexMessageTriggerSpec extends MessageTriggerSpec {

  def triggerFor(pattern: String, requiresBotMention: Boolean = false, isCaseSensitive: Boolean = true): RegexMessageTrigger = {
    val team = Team(IDs.next, "Team!", None)
    val versionId = IDs.next
    val group = BehaviorGroup(IDs.next, "", None, None, None, team, None, OffsetDateTime.now)
    val groupVersion = BehaviorGroupVersion(IDs.next, group, "", None, None, None, OffsetDateTime.now)
    val behavior = Behavior(IDs.next, team, Some(group), Some(versionId), None, OffsetDateTime.now)
    val behaviorVersion = BehaviorVersion(versionId, behavior, groupVersion, None, None, None, None, forcePrivateResponse = false, None, OffsetDateTime.now)
    RegexMessageTrigger(IDs.next, behaviorVersion, pattern, requiresBotMention, isCaseSensitive)
  }

  val oneParamPattern = """deploy\s+(\S+)"""
  val twoParamPattern = """deploy\s+(\S+)\s+(\S+)"""

  "RegexMessageTrigger" should {

    "be activated with one word param" in  {
      val trigger = triggerFor(oneParamPattern)
      matches(trigger, "deploy foo") mustBe true
    }

    "respect case when instructed" in {
      val trigger = triggerFor(oneParamPattern, isCaseSensitive = true)
      matches(trigger, "Deploy foo") mustBe false
    }

    "ignore case when instructed" in {
      val trigger = triggerFor(oneParamPattern, isCaseSensitive = false)
      matches(trigger, "Deploy foo") mustBe true
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

    "doesn't blow up with an invalid regex pattern but is never activated" in {
      val trigger = triggerFor("""yo {yo} yo""")
      matches(trigger, "yo {yo} yo") mustBe false
      trigger.isValidRegex mustBe false
    }

    "build an invocation parameter" in {
      val trigger = triggerFor(oneParamPattern)
      val params = Seq(newParameterFor("version", 1, trigger.behaviorVersion))
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345")
    }

    "build two invocation parameters" in {
      val trigger = triggerFor(twoParamPattern)
      val params = Seq(
        newParameterFor("version", 1, trigger.behaviorVersion),
        newParameterFor("subversion", 2, trigger.behaviorVersion)
      )
      val invocationParams = trigger.invocationParamsFor("deploy ellipsis-12345 0.0.1", params)
      invocationParams mustBe Map("param0" -> "ellipsis-12345", "param1" -> "0.0.1")
    }

  }

}
