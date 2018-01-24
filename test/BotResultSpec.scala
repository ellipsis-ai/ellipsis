import akka.actor.ActorSystem
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{SlackMessage, SlackMessageEvent}
import models.behaviors.{NoResponseResult, SuccessResult}
import models.team.Team
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsString}
import support.DBSpec
import utils.SlackTimestamp

import scala.concurrent.Future

class BotResultSpec extends PlaySpec with MockitoSugar with DBSpec {

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
    SlackMessageEvent(profile, defaultChannel, maybeThreadId, defaultSlackUserId, SlackMessage.blank, None, SlackTimestamp.now, slackEventService.clientFor(profile), None)
  }

  def newConversationFor(team: Team, user: User, profile: SlackBotProfile, event: SlackMessageEvent): Conversation = {
    val group = newSavedBehaviorGroupFor(team)

    val groupVersion = newSavedGroupVersionFor(group, user)
    val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head

    runNow(InvokeBehaviorConversation.createFor(behaviorVersion, newEventFor(profile), Some(event.channel), None, dataService))
  }

  def mockPostChatMessage(text: String, event: SlackMessageEvent, resultTs: String, maybeThreadId: Option[String]): Unit = {
    when(event.client.postChatMessage(
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

  "sendIn" should {

    "send a response" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head

        val event: SlackMessageEvent = newEventFor(profile)
        val responseText = "response"
        val result = SuccessResult(event, None, JsString("result"), JsNull, Seq(), Some(responseText), None, forcePrivateResponse = false, isForUndeployed = false)
        val resultTs: String = SlackTimestamp.now

        mockPostChatMessage(responseText, event, resultTs, None)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)
      })
    }

    "interrupt ongoing conversations" in {
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)

        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val resultJs = JsString("result")
        val result = SuccessResult(event, None, resultJs, JsNull, Seq(), Some(responseText), None, forcePrivateResponse = false, isForUndeployed = false)
        val resultTs: String = SlackTimestamp.now

        val conversationToBeInterrupted = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, resultTs, None)
        mockPostChatMessage(resultJs.toString, event, resultTs, Some(resultTs))
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, resultTs, None)

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
      withEmptyDB(dataService, { () =>
        val profile = newSavedBotProfile
        val team = runNow(dataService.teams.find(profile.teamId)).head
        val user = newSavedUserOn(team)
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val result = NoResponseResult(event, None, JsNull, None)
        val resultTs: String = SlackTimestamp.now

        val conversation = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, resultTs, None)

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
        val event: SlackMessageEvent = newEventFor(profile)

        val responseText = "response"
        val resultJs = JsString("result")
        val resultTs: String = SlackTimestamp.now

        val selfConversation = newConversationFor(team, user, profile, event)
        val conversationToInterrupt = newConversationFor(team, user, profile, event)

        val result = SuccessResult(event, Some(selfConversation), resultJs, JsNull, Seq(), Some(responseText), None, forcePrivateResponse = false, isForUndeployed = false)

        mockPostChatMessage(responseText, event, resultTs, None)
        mockPostChatMessage(resultJs.toString, event, resultTs, Some(resultTs))
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, resultTs, None)

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
        val event: SlackMessageEvent = newEventFor(profile, Some(threadId))


        val responseText = "response"
        val resultTs: String = SlackTimestamp.now

        var threadedConversation = newConversationFor(team, user, profile, event)
        runNow(dataService.conversations.save(threadedConversation.copyWithMaybeThreadId(Some(threadId))))
        threadedConversation = runNow(dataService.conversations.find(threadedConversation.id)).get

        val result = SuccessResult(event, Some(threadedConversation), JsString("result"), JsNull, Seq(), Some(responseText), None, forcePrivateResponse = false, isForUndeployed = false)

        val otherConversation = newConversationFor(team, user, profile, event)

        mockPostChatMessage(responseText, event, resultTs, Some(threadId))
        val interruptionPrompt = dataService.conversations.interruptionPromptFor(event, result.interruptionPrompt, includeUsername = true)
        mockPostChatMessage(interruptionPrompt, event, SlackTimestamp.now, None)

        runNow(botResultService.sendIn(result, None)) mustBe Some(resultTs)

        val updatedOtherConversation = runNow(dataService.conversations.find(otherConversation.id)).get
        updatedOtherConversation.maybeThreadId.isEmpty mustBe true
      })
    }

  }
}
