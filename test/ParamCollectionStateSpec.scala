import json.BehaviorParameterData
import models.behaviors.conversations.{InvokeBehaviorConversation, ParamCollectionState}
import models.behaviors.events.{MessageContext, MessageEvent}
import models.behaviors.testing.TestMessageContext
import support.DBSpec

class ParamCollectionStateSpec extends DBSpec {

  "ParamCollectionState" should {

    "Use a saved answer (for team) if present and input configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = new MessageEvent {
          val context: MessageContext = TestMessageContext(user, team, "foo", true)
        }
        val group = runNow(dataService.behaviorGroups.createFor("", "", None, team))
        val behavior = runNow(dataService.behaviors.createFor(group, None, None))
        val version = runNow(dataService.behaviorVersions.createFor(behavior, None))
        val trigger = runNow(dataService.messageTriggers.createFor(version, "foo", false, false, false))
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event.context.name, event.context.userIdForContext, trigger, dataService))
        val param = runNow(dataService.behaviorParameters.ensureFor(version, Seq(BehaviorParameterData("param", None, "", isSavedForTeam = Some(true), isSavedForUser = None, None, None)))).head
        val savedAnswer = runNow(dataService.savedAnswers.ensureFor(param.input, "answer", user))
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe None
      })
    }

    "Use a saved answer (for user) if present and input configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = new MessageEvent {
          val context: MessageContext = TestMessageContext(user, team, "foo", true)
        }
        val group = runNow(dataService.behaviorGroups.createFor("", "", None, team))
        val behavior = runNow(dataService.behaviors.createFor(group, None, None))
        val version = runNow(dataService.behaviorVersions.createFor(behavior, None))
        val trigger = runNow(dataService.messageTriggers.createFor(version, "foo", false, false, false))
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event.context.name, event.context.userIdForContext, trigger, dataService))
        val param = runNow(dataService.behaviorParameters.ensureFor(version, Seq(BehaviorParameterData("param", None, "", isSavedForTeam = None, isSavedForUser = Some(true), None, None)))).head
        val savedAnswer = runNow(dataService.savedAnswers.ensureFor(param.input, "answer", user))
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe None
      })
    }

    "don't use a saved answer if present but input not configured for it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = new MessageEvent {
          val context: MessageContext = TestMessageContext(user, team, "foo", true)
        }
        val group = runNow(dataService.behaviorGroups.createFor("", "", None, team))
        val behavior = runNow(dataService.behaviors.createFor(group, None, None))
        val version = runNow(dataService.behaviorVersions.createFor(behavior, None))
        val trigger = runNow(dataService.messageTriggers.createFor(version, "foo", false, false, false))
        val conversation = runNow(InvokeBehaviorConversation.createFor(version, event.context.name, event.context.userIdForContext, trigger, dataService))
        val param = runNow(dataService.behaviorParameters.ensureFor(version, Seq(BehaviorParameterData("param", None, "", isSavedForTeam = None, isSavedForUser = None, None, None)))).head
        val savedAnswer = runNow(dataService.savedAnswers.ensureFor(param.input, "answer", user))
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe Some(param.id)
      })
    }

  }

}
