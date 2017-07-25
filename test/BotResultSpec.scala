import akka.actor.ActorSystem
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageEvent
import models.behaviors.{NoResponseResult, SuccessResult}
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
import slack.api.SlackApiClient
import support.DBSpec
import utils.SlackTimestamp

import scala.concurrent.Future

class BotResultSpec extends PlaySpec with MockitoSugar with DBSpec {

  val defaultContext = "slack"
  val defaultChannel = "C1234567"
  val defaultSlackTeamId = "T1234567"
  val defaultSlackUserId = "U1234567"
  val defaultSlackBotUserId = "U3456789"
  val defaultSlackToken = IDs.next
  val defaultThreadId: Option[String] = None

  implicit val system = ActorSystem("slack")

  def newSavedBotProfile: SlackBotProfile = {
    runNow(dataService.slackBotProfiles.ensure(IDs.next, IDs.next, IDs.next, IDs.next))
  }

  def newEventFor(profile: SlackBotProfile, maybeThreadId: Option[String] = defaultThreadId): SlackMessageEvent = {
    SlackMessageEvent(profile, defaultChannel, maybeThreadId, defaultSlackUserId, "", SlackTimestamp.now, mock[SlackApiClient])
  }

  def newMockConversation(maybeThreadId: Option[String] = None): Conversation = {
    val convo = mock[Conversation]
    when(convo.id).thenReturn(IDs.next)
    when(convo.maybeThreadId).thenReturn(maybeThreadId)
    when(convo.maybeChannel).thenReturn(Some(defaultChannel))
    convo
  }

  def mockPostChatMessage(text: String, client: SlackApiClient, resultTs: String, maybeThreadId: Option[String]): Unit = {
    println(s"mocking text: $text ts: $resultTs threadId: $maybeThreadId")
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
      replyBroadcast = Some(false)
    )).thenReturn({
      Future.successful(resultTs)
    })
  }

  def mockSlackClient(event: SlackMessageEvent): Unit = {
    val client = event.client
    when(slackEventService.clientFor(event.profile)).thenReturn(client)
    when(client.listIms).thenReturn(Future.successful(Seq()))
    when(client.getUserInfo(anyString)(any[ActorSystem]())).thenReturn(Future.successful(mock[slack.models.User]))
  }

  "sendIn" should {

    "send a response" in {
      withEmptyDB(dataService, { db =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)

        val event: SlackMessageEvent = newEventFor(profile)
        val responseText = "response"
        val result = SuccessResult(event, None, JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)
        val resultTs: String = SlackTimestamp.now

        mockSlackClient(event)
        mockPostChatMessage(responseText, event.client, resultTs, None)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)
      })
    }

    "interrupt ongoing conversations" in {
      withEmptyDB(dataService, { db =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)

        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val result = SuccessResult(event, None, JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)
        val resultTs: String = SlackTimestamp.now

        val group = newSavedBehaviorGroupFor(team)

        val groupVersion = newSavedGroupVersionFor(group, user)
        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val conversationToBeInterrupted = runNow(InvokeBehaviorConversation.createFor(behaviorVersion, newEventFor(profile), Some(event.channel), None, dataService))

        mockSlackClient(event)
        mockPostChatMessage(responseText, event.client, resultTs, None)
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event.client, resultTs, None)

        conversationToBeInterrupted.maybeThreadId.isEmpty mustBe true
        val ongoing = runNow(dataService.conversations.allOngoingFor(event.userIdForContext, event.context, Some(event.channel), event.maybeThreadId))
        ongoing must have length(1)
        ongoing.head mustBe conversationToBeInterrupted

        runNow(dataService.slackBotProfiles.allFor(team)) mustBe Seq(profile)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        val updatedConversation = runNow(dataService.conversations.find(conversationToBeInterrupted.id)).get

        updatedConversation.maybeThreadId.isDefined mustBe true
      })
    }

    "not interrupt for noResponse()" in {
      withEmptyDB(dataService, { db =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val result = NoResponseResult(event, None, None)
        val resultTs: String = SlackTimestamp.now

        val conversation = newMockConversation()

        mockSlackClient(event)
        mockPostChatMessage(responseText, event.client, resultTs, None)

        when(dataService.conversations.background(conversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

        runNow(botResultService.sendIn(result, None)) mustBe None

        Mockito.verify(dataService.conversations, times(0)).background(conversation, result.interruptionPrompt, true)
      })
    }

    "not interrupt self conversation" in {
      withEmptyDB(dataService, { db =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val resultTs: String = SlackTimestamp.now

        val selfConversation = newMockConversation()
        val otherConversation = newMockConversation()

        val result = SuccessResult(event, Some(selfConversation), JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)

        mockSlackClient(event)
        mockPostChatMessage(responseText, event.client, resultTs, None)

        when(dataService.conversations.background(selfConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))
        when(dataService.conversations.background(otherConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        Mockito.verify(dataService.conversations, times(0)).background(selfConversation, result.interruptionPrompt, true)
        Mockito.verify(dataService.conversations, times(1)).background(otherConversation, result.interruptionPrompt, true)
      })
    }

    "not interrupt for message in thread" in {
      withEmptyDB(dataService, { db =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head

        val threadId = IDs.next
        val event: SlackMessageEvent = newEventFor(profile, Some(threadId))

        val responseText = "response"
        val resultTs: String = SlackTimestamp.now

        val threadedConversation = newMockConversation(Some(threadId))
        val otherConversation = newMockConversation()

        val result = SuccessResult(event, Some(threadedConversation), JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)

        mockSlackClient(event)
        mockPostChatMessage(responseText, event.client, resultTs, Some(threadId))

        when(dataService.conversations.background(threadedConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))
        when(dataService.conversations.background(otherConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        Mockito.verify(dataService.conversations, times(0)).background(threadedConversation, result.interruptionPrompt, true)
        Mockito.verify(dataService.conversations, times(0)).background(otherConversation, result.interruptionPrompt, true)
      })
    }

  }
}
