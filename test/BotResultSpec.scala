import akka.actor.ActorSystem
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
import models.behaviors.events.SlackEventContext
import models.behaviors.{DeveloperContext, NoResponseForBehaviorVersionResult, SuccessResult}
import models.team.Team
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsObject, JsString}
import services.slack.{SlackApiClient, SlackEventService}
import support.{DBSpec, SlackContext}
import utils.SlackTimestamp

import scala.concurrent.Future

class BotResultSpec extends PlaySpec with MockitoSugar with DBSpec with SlackContext {

  val defaultContext = "slack"
  val defaultChannel = "C1234567"
  val defaultSlackTeamId = "T1234567"
  val defaultSlackUserId = "U1234567"
  val defaultSlackBotUserId = "U3456789"
  val defaultSlackToken: String = IDs.next
  val defaultThreadId: Option[String] = None

  implicit val system = ActorSystem("slack")

  def newSavedBotProfile: SlackBotProfile = {
    runNow(dataService.slackBotProfiles.ensure(IDs.next, IDs.next, IDs.next, IDs.next))
  }

  def newEventFor(profile: SlackBotProfile, maybeThreadId: Option[String] = defaultThreadId): SlackMessageEvent = {
    SlackMessageEvent(
      SlackEventContext(
        profile,
        defaultChannel,
        maybeThreadId,
        defaultSlackUserId
      ),
      SlackMessage.blank,
      maybeFile = None,
      maybeTs = None,
      maybeOriginalEventType = None,
      maybeScheduled = None,
      isUninterruptedConversation = false,
      isEphemeral = false,
      maybeResponseUrl = None,
      beQuiet = false
    )
  }

  def newConversationFor(team: Team, user: User, profile: SlackBotProfile, event: SlackMessageEvent): Conversation = {
    val group = newSavedBehaviorGroupFor(team)

    val groupVersion = newSavedGroupVersionFor(group, user)
    val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head

    runNow(InvokeBehaviorConversation.createFor(behaviorVersion, newEventFor(profile), Some(event.channel), None, None, None, services))
  }

  def mockPostChatMessage(text: String, event: SlackMessageEvent, client: SlackApiClient, resultTs: String, maybeThreadId: Option[String], slackEventService: SlackEventService): Unit = {
    val message = SlackMessage.fromUnformattedText(text, client.profile, Some(resultTs), maybeThreadId)
    when(client.postChatMessage(
      channelId = defaultChannel,
      text = text,
      username = None,
      asUser = Some(true),
      parse = None,
      linkNames = None,
      attachments = None,
      unfurlLinks = Some(false),
      unfurlMedia = Some(true),
      iconUrl = None,
      iconEmoji = None,
      replaceOriginal = None,
      deleteOriginal = None,
      threadTs = maybeThreadId,
      replyBroadcast = None
    )).thenReturn(Future.successful(message))
  }

  "sendIn" should {

    "send a response" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head

        val event: SlackMessageEvent = newEventFor(profile)
        val responseText = "response"
        val client = newMockSlackApiClientFor(slackApiService, profile, defaultChannel)
        val result =
          SuccessResult(
            event = event,
            behaviorVersion = mock[BehaviorVersion],
            maybeConversation = None,
            result = JsString("result"),
            payloadJson = JsNull,
            parametersWithValues = Seq(),
            invocationJson = JsObject.empty,
            maybeResponseTemplate = Some(responseText),
            maybeLogResult = None,
            responseType = Normal,
            isForCopilot = false,
            developerContext = DeveloperContext.default,
            dataService
          )
        val resultTs: String = SlackTimestamp.now

        mockPostChatMessage(responseText, event, client, resultTs, None, slackEventService)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)
      })
    }

    "interrupt ongoing conversations" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)

        val client = newMockSlackApiClientFor(slackApiService, profile, defaultChannel)
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val resultJs = JsString("result")
        val result =
          SuccessResult(
            event = event,
            behaviorVersion = mock[BehaviorVersion],
            maybeConversation = None,
            result = resultJs,
            payloadJson = JsNull,
            parametersWithValues = Seq(),
            invocationJson = JsObject.empty,
            maybeResponseTemplate = Some(responseText),
            maybeLogResult = None,
            responseType = Normal,
            isForCopilot = false,
            developerContext = DeveloperContext.default,
            dataService
          )
        val resultTs: String = SlackTimestamp.now

        val conversationToBeInterrupted = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, client, resultTs, None, slackEventService)
        mockPostChatMessage(resultJs.value, event, client, resultTs, Some(resultTs), slackEventService)
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, client, resultTs, None, slackEventService)

        conversationToBeInterrupted.maybeThreadId.isEmpty mustBe true
        val ongoing = runNow(dataService.conversations.allOngoingFor(event.eventContext, None))
        ongoing must have length(1)
        ongoing.head mustBe conversationToBeInterrupted

        runNow(dataService.slackBotProfiles.allFor(team)) mustBe Seq(profile)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        val updatedConversation = runNow(dataService.conversations.find(conversationToBeInterrupted.id)).get

        updatedConversation.maybeThreadId.isDefined mustBe true
      })
    }

    "not interrupt for noResponse()" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)
        val client = newMockSlackApiClientFor(slackApiService, profile, defaultChannel)
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val result = NoResponseForBehaviorVersionResult(event, mock[BehaviorVersion], None, JsNull, None, isForCopilot = false)
        val resultTs: String = SlackTimestamp.now

        val conversation = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, client, resultTs, None, slackEventService)

        runNow(botResultService.sendIn(result, None)) mustBe None

        val updatedConversation = runNow(dataService.conversations.find(conversation.id)).get

        updatedConversation.maybeThreadId.isEmpty mustBe true
      })
    }

    "not interrupt self conversation" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)
        val client = newMockSlackApiClientFor(slackApiService, profile, defaultChannel)
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val resultJs = JsString("result")
        val resultTs: String = SlackTimestamp.now

        val selfConversation = newConversationFor(team, user, profile, event)
        val conversationToInterrupt = newConversationFor(team, user, profile, event)

        val result =
          SuccessResult(
            event = event,
            behaviorVersion = mock[BehaviorVersion],
            maybeConversation = Some(selfConversation),
            result = resultJs,
            payloadJson = JsNull,
            parametersWithValues = Seq(),
            invocationJson = JsObject.empty,
            maybeResponseTemplate = Some(responseText),
            maybeLogResult = None,
            responseType = Normal,
            isForCopilot = false,
            developerContext = DeveloperContext.default,
            dataService
          )

        mockPostChatMessage(responseText, event, client, resultTs, None, slackEventService)
        mockPostChatMessage(resultJs.value, event, client, resultTs, Some(resultTs), slackEventService)
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, client, resultTs, None, slackEventService)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        val updatedSelfConversation = runNow(dataService.conversations.find(selfConversation.id)).get
        updatedSelfConversation.maybeThreadId.isEmpty mustBe true

        val updatedConversationToInterrupt = runNow(dataService.conversations.find(conversationToInterrupt.id)).get
        updatedConversationToInterrupt.maybeThreadId.isDefined mustBe true
      })
    }

    "not interrupt for message in thread" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)

        val threadId = IDs.next
        val client = newMockSlackApiClientFor(slackApiService, profile, defaultChannel)
        val event: SlackMessageEvent = newEventFor(profile, Some(threadId))


        val responseText = "response"
        val resultTs: String = SlackTimestamp.now

        var threadedConversation = newConversationFor(team, user, profile, event)
        runNow(dataService.conversations.save(threadedConversation.copyWithMaybeThreadId(Some(threadId))))
        threadedConversation = runNow(dataService.conversations.find(threadedConversation.id)).get

        val result =
          SuccessResult(
            event = event,
            behaviorVersion = mock[BehaviorVersion],
            maybeConversation = Some(threadedConversation),
            result = JsString("result"),
            payloadJson = JsNull,
            parametersWithValues = Seq(),
            invocationJson = JsObject.empty,
            maybeResponseTemplate = Some(responseText),
            maybeLogResult = None,
            responseType = Normal,
            isForCopilot = false,
            developerContext = DeveloperContext.default,
            dataService
          )

        val otherConversation = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, client, resultTs, Some(threadId), slackEventService)
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, client, SlackTimestamp.now, None, slackEventService)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        val updatedOtherConversation = runNow(dataService.conversations.find(otherConversation.id)).get
        updatedOtherConversation.maybeThreadId.isEmpty mustBe true
      })
    }

  }
}
