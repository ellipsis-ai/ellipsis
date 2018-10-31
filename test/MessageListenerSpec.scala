import json.BehaviorVersionData
import models.behaviors.events.TestEventContext
import models.behaviors.testing.TestMessageEvent
import support.DBSpec

class MessageListenerSpec extends DBSpec {

  "MessageListener" should {

    "create responses for events in the right context" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val event = TestMessageEvent(TestEventContext(user, team), "foo", includesBotMention = false)
        val group = newSavedBehaviorGroupFor(team)

        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, isTest = false, maybeName = None, dataService)
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        runNow(dataService.behaviorGroupDeployments.deploy(groupVersion, user.id, None))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head

        val channel = event.maybeChannel.get
        runNow(dataService.messageListeners.createFor(behaviorVersion.behavior, Map(), user, team, event.context, channel, None))

        val responses = runNow(event.allBehaviorResponsesFor(Some(team), None, services))

        responses must have length(1)
        responses.head.behaviorVersion mustBe behaviorVersion
      })
    }

  }

}
