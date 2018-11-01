import json.BehaviorVersionData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.{SlackEventContext, SlackMessage, SlackMessageEvent}
import support.DBSpec
import utils.SlackTimestamp

class MessageListenerSpec extends DBSpec {

  "MessageListener" should {

    "create responses for events in the right context" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val botProfile = mock[SlackBotProfile]
        val channel = "C123456"
        val slackUserId = "U123456"
        val event = SlackMessageEvent(
          SlackEventContext(
            botProfile,
            channel,
            None,
            slackUserId
          ),
          SlackMessage.fromUnformattedText("foo", botProfile),
          None,
          SlackTimestamp.now,
          None,
          false,
          false,
          None,
          false
        )
        val group = newSavedBehaviorGroupFor(team)

        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, isTest = false, maybeName = None, dataService)
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        runNow(dataService.behaviorGroupDeployments.deploy(groupVersion, user.id, None))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head

        runNow(dataService.messageListeners.createFor(behaviorVersion.behavior, Map(), user, team, event.eventContext.name, channel, None))

        val responses = runNow(event.allBehaviorResponsesFor(Some(team), None, services))

        responses must have length(1)
        responses.head.behaviorVersion mustBe behaviorVersion
      })
    }

  }

}
