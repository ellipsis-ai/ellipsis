import json.BehaviorVersionData
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

        val inputData = newInputDataFor(isSavedForTeam = Some(true))
        val triggerData = newTriggerData
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get),
          triggers = Seq(triggerData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val trigger = runNow(dataService.messageTriggers.allFor(behaviorVersion)).head
        val conversation = runNow(InvokeBehaviorConversation.createFor(behaviorVersion, event, event.maybeChannel, Some(trigger), dataService))
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head

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

        val inputData = newInputDataFor(isSavedForUser = Some(true))
        val triggerData = newTriggerData
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get),
          triggers = Seq(triggerData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val trigger = runNow(dataService.messageTriggers.allFor(behaviorVersion)).head
        val conversation = runNow(InvokeBehaviorConversation.createFor(behaviorVersion, event, event.maybeChannel, Some(trigger), dataService))
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head

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

        val inputData = newInputDataFor()
        val triggerData = newTriggerData
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get),
          triggers = Seq(triggerData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val trigger = runNow(dataService.messageTriggers.allFor(behaviorVersion)).head
        val conversation = runNow(InvokeBehaviorConversation.createFor(behaviorVersion, event, event.maybeChannel, Some(trigger), dataService))
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head

        val savedAnswer = newSavedAnswerFor(param.input, user)
        val state = runNow(ParamCollectionState.from(conversation, event, dataService, cache, configuration))

        runNow(state.maybeNextToCollect(conversation)).map(_._1.id) mustBe Some(param.id)
      })
    }

  }

}
