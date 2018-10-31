package legacyjson

import json.{BehaviorTriggerData, LegacyBehaviorTriggerJson}
import models.behaviors.triggers.{MessageSent, ReactionAdded}
import org.scalatestplus.play.PlaySpec

class LegacyBehaviorTriggerJsonSpec extends PlaySpec {
  "LegacyBehaviorTriggerJson" should {
    "create BehaviorTriggerData with triggerType passed through if provided" in {
      val json = LegacyBehaviorTriggerJson(":tada:", requiresMention = true, isRegex = false, caseSensitive = false, triggerType = Some(ReactionAdded.toString))
      json.toBehaviorTriggerData mustEqual BehaviorTriggerData(":tada:", requiresMention = true, isRegex = false, caseSensitive = false, triggerType = ReactionAdded.toString)
    }

    "create BehaviorTriggerData with triggerType default as MessageSent" in {
      val json = LegacyBehaviorTriggerJson(":tada:", requiresMention = true, isRegex = false, caseSensitive = false, triggerType = None)
      json.toBehaviorTriggerData mustEqual BehaviorTriggerData(":tada:", requiresMention = true, isRegex = false, caseSensitive = false, triggerType = MessageSent.toString)
    }
  }
}
