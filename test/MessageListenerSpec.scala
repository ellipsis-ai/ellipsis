import json.BehaviorVersionData
import models.behaviors.testing.TestEvent
import support.DBSpec

class MessageListenerSpec extends DBSpec {

  "MessageListener" should {

    "create responses for events in the right context" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = TestEvent(user, team, "foo", includesBotMention = false)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor(isSavedForTeam = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, isTest = false, maybeName = None, dataService).copy(
          inputIds = Seq(inputData.inputId.get),
          triggers = Seq()
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        runNow(dataService.behaviorGroupDeployments.deploy(groupVersion, user.id, None))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head

        val channel = event.maybeChannel.get
        val messageInput = runNow(dataService.inputs.findByInputId(inputData.inputId.get, groupVersion)).get
        runNow(dataService.messageListeners.createFor(behaviorVersion.behavior, messageInput, Map(), user, team, event.context, channel, None))

        val responses = runNow(event.allBehaviorResponsesFor(Some(team), None, services))

        responses must have length(1)
        responses.head.behaviorVersion mustBe behaviorVersion
      })
    }

  }

}
