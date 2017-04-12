import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.{NoResponseResult, SuccessResult}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageEvent
import models.team.Team
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
import services.DataService
import slack.api.SlackApiClient
import support.TestContext
import utils.SlackTimestamp

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BotResultSpec extends PlaySpec with MockitoSugar {

  val defaultContext = "slack"
  val defaultChannel = "C1234567"
  val defaultSlackTeamId = "T1234567"
  val defaultSlackUserId = "U1234567"
  val defaultSlackBotUserId = "U3456789"
  val defaultSlackToken = IDs.next
  val defaultThreadId: Option[String] = None

  implicit val system = ActorSystem("slack")

  def runNow[T](f: Future[T]) = Await.result(f, 30.seconds)

  def newEventFor(team: Team): SlackMessageEvent = {
    val profile = SlackBotProfile(defaultSlackBotUserId, team.id, defaultSlackTeamId, defaultSlackToken, OffsetDateTime.now)
    SlackMessageEvent(profile, defaultChannel, defaultThreadId, defaultSlackUserId, "", SlackTimestamp.now)
  }

  def newMockConversation: Conversation = {
    val convo = mock[Conversation]
    when(convo.id).thenReturn(IDs.next)
    when(convo.maybeThreadId).thenReturn(None)
    when(convo.maybeChannel).thenReturn(Some(defaultChannel))
    convo
  }

  def setUpMocks(
                  event: SlackMessageEvent,
                  responseText: String,
                  resultTs: String,
                  ongoingConversations: Seq[Conversation],
                  dataService: DataService
                ): Unit = {
    val slackClient = mock[SlackApiClient]
    when(dataService.slackBotProfiles.clientFor(event.profile)).thenReturn(slackClient)
    when(slackClient.listIms).thenReturn(Future.successful(Seq()))
    when(dataService.conversations.allOngoingFor(defaultSlackUserId, event.context, event.maybeChannel, event.maybeThreadId)).thenReturn(Future.successful(ongoingConversations))

    when(slackClient.postChatMessage(
      defaultChannel,
      responseText,
      None,
      Some(true),
      None,
      None,
      None,
      Some(false),
      Some(true),
      None,
      None,
      None,
      None,
      None,
      Some(false)
    )).thenReturn(Future.successful(resultTs))

  }

  "sendIn" should {

    "send a response" in new TestContext {
      val event: SlackMessageEvent = newEventFor(team)
      val responseText = "response"
      val result = SuccessResult(event, None, JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)
      val resultTs: String = SlackTimestamp.now

      setUpMocks(event, responseText, resultTs, Seq(), dataService)

      runNow(result.sendIn(None, None, dataService)) mustBe Some(resultTs)
    }

    "interrupt ongoing conversations" in new TestContext {
      val event: SlackMessageEvent = newEventFor(team)
      val responseText = "response"
      val result = SuccessResult(event, None, JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)
      val resultTs: String = SlackTimestamp.now

      val conversation = newMockConversation

      setUpMocks(event, responseText, resultTs, Seq(conversation), dataService)

      when(dataService.conversations.background(conversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

      runNow(result.sendIn(None, None, dataService)) mustBe Some(resultTs)

      Mockito.verify(dataService.conversations, times(1)).background(conversation, result.interruptionPrompt, true)
    }

    "not interrupt for noResponse()" in new TestContext {
      val event: SlackMessageEvent = newEventFor(team)
      val responseText = "response"
      val result = NoResponseResult(event, None, None)
      val resultTs: String = SlackTimestamp.now

      val conversation = newMockConversation

      setUpMocks(event, responseText, resultTs, Seq(conversation), dataService)

      when(dataService.conversations.background(conversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

      runNow(result.sendIn(None, None, dataService)) mustBe None

      Mockito.verify(dataService.conversations, times(0)).background(conversation, result.interruptionPrompt, true)
    }

    "not interrupt self conversation" in new TestContext {
      val event: SlackMessageEvent = newEventFor(team)
      val responseText = "response"
      val resultTs: String = SlackTimestamp.now

      val selfConversation = newMockConversation
      val otherConversation = newMockConversation

      val result = SuccessResult(event, Some(selfConversation), JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)

      setUpMocks(event, responseText, resultTs, Seq(selfConversation, otherConversation), dataService)

      when(dataService.conversations.background(selfConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))
      when(dataService.conversations.background(otherConversation, result.interruptionPrompt, true)).thenReturn(Future.successful({}))

      runNow(result.sendIn(None, Some(selfConversation), dataService)) mustBe Some(resultTs)

      Mockito.verify(dataService.conversations, times(0)).background(selfConversation, result.interruptionPrompt, true)
      Mockito.verify(dataService.conversations, times(1)).background(otherConversation, result.interruptionPrompt, true)
    }

  }
}
