import models.behaviors.conversations.{InvokeBehaviorConversation, ParamCollectionState}
import models.behaviors.testing.TestEvent
import support.DBSpec

class ParamCollectionStateSpec extends DBSpec {

  "ParamCollectionState" should {

    "Use a saved answer (for team) if present and input configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = TestEvent(user, team, "foo", includesBotMention = false)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val trigger = newSavedTriggerFor(version)
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event, event.maybeChannel, trigger, dataService))
        val param = newSavedParamFor(version, isSavedForTeam = Some(true))
        val savedAnswer = newSavedAnswerFor(param.input, user)
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe None
      })
    }

    "Use a saved answer (for user) if present and input configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = TestEvent(user, team, "foo", includesBotMention = false)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val trigger = newSavedTriggerFor(version)
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event, event.maybeChannel, trigger, dataService))
        val param = newSavedParamFor(version, isSavedForUser = Some(true))
        val savedAnswer = newSavedAnswerFor(param.input, user)
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe None
      })
    }

    "don't use a saved answer if present but input not configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = TestEvent(user, team, "foo", includesBotMention = false)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val trigger = newSavedTriggerFor(version)
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event, event.maybeChannel, trigger, dataService))
        val param = newSavedParamFor(version)
        val savedAnswer = newSavedAnswerFor(param.input, user)
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe Some(param.id)
      })
    }

  }

}
