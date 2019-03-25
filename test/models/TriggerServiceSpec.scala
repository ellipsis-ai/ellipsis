package models

import json.BehaviorTriggerData
import models.behaviors.triggers.{MessageSent, ReactionAdded}
import support.DBSpec

class TriggerServiceSpec extends DBSpec {
  "TriggerService.createTriggersForAction" should {
    "create a list of triggers for a behavior version with empty ones and duplicates removed" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupVersion = newSavedGroupVersionFor(group, user)
        val behavior = newSavedBehaviorFor(group)
        val behaviorVersion = newSavedBehaviorVersionFor(behavior, groupVersion, user)
        val triggersData = Seq(
          BehaviorTriggerData("foo", requiresMention = true, isRegex = false, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("foo", requiresMention = false, isRegex = true, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("bar", requiresMention = true, isRegex = false, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("foo", requiresMention = true, isRegex = false, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("", requiresMention = false, isRegex = false, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("bar", requiresMention = false, isRegex = false, caseSensitive = false, MessageSent.toString),
          BehaviorTriggerData("foo", requiresMention = true, isRegex = false, caseSensitive = false, ReactionAdded.toString),
          BehaviorTriggerData("tada", requiresMention = true, isRegex = false, caseSensitive = false, ReactionAdded.toString),
          BehaviorTriggerData("banana", requiresMention = true, isRegex = false, caseSensitive = false, ReactionAdded.toString),
          BehaviorTriggerData("", requiresMention = true, isRegex = false, caseSensitive = false, ReactionAdded.toString),
          BehaviorTriggerData("tada", requiresMention = true, isRegex = false, caseSensitive = false, ReactionAdded.toString)
        )
        val triggers = runNow(dataService.triggers.createTriggersForAction(behaviorVersion, triggersData))
        triggers.length mustEqual(7)
        triggers(0).pattern mustEqual "foo"
        triggers(0).shouldTreatAsRegex mustBe false
        triggers(0).triggerType mustEqual MessageSent

        triggers(1).pattern mustEqual "foo"
        triggers(1).shouldTreatAsRegex mustBe true
        triggers(1).triggerType mustEqual MessageSent

        triggers(2).pattern mustEqual "bar"
        triggers(2).requiresBotMention mustBe true

        triggers(3).pattern mustEqual "bar"
        triggers(3).requiresBotMention mustBe false

        triggers(4).pattern mustEqual "foo"
        triggers(4).triggerType mustEqual ReactionAdded

        triggers(5).pattern mustEqual "tada"
        triggers(5).triggerType mustEqual ReactionAdded

        triggers(6).pattern mustEqual "banana"
        triggers(6).triggerType mustEqual ReactionAdded
      })
    }
  }
}
