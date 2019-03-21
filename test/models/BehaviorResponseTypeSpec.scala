package models

import models.behaviors.behaviorversion.{BehaviorVersion, Normal, Private, Threaded}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, DeveloperContext, SuccessResult}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsString}
import services.DataService
import utils.SlackTimestamp

class BehaviorResponseTypeSpec extends PlaySpec with MockitoSugar {
  val originatingChannel = "CHANNEL"
  val dmChannelToUse = "DCHANNEL"
  val conversationChannel = "CONVERSATION"
  val originalMessageTs = "101010101010"
  val threadTs = "999888777666"
  val conversationThreadTs = "111222333444"
  val dmChannel = "D1234567"

  def botResultFor(event: Event, maybeConversation: Option[Conversation]): BotResult = {
    val behaviorVersion = mock[BehaviorVersion]
    val mockDataService = mock[DataService]
    SuccessResult(event, behaviorVersion, maybeConversation, JsString("Success!"), JsObject.empty, Seq.empty, JsObject.empty, None, None, Threaded, DeveloperContext.default, mockDataService)
  }


  "channelToUseFor" should {
    "Prioritize the originating channel if a thread is present and not private-with-DM" in {
      val conversation = mock[Conversation]

      Normal.channelToUseFor(originatingChannel, Some(conversation), Some(threadTs), Some(dmChannel)) mustBe originatingChannel
      Threaded.channelToUseFor(originatingChannel, Some(conversation), Some(threadTs), Some(dmChannel)) mustBe originatingChannel
    }

    "Prioritize the conversation channel if no thread is present" in {
      val conversation = mock[Conversation]
      when(conversation.maybeChannel).thenReturn(Some(conversationChannel))

      Normal.channelToUseFor(originatingChannel, Some(conversation), None, Some(dmChannel)) mustBe conversationChannel
      Threaded.channelToUseFor(originatingChannel, Some(conversation), None, Some(dmChannel)) mustBe conversationChannel
    }

    "Return the originating channel if no thread or conversation channel is present" in {
      val conversationWithoutChannel = mock[Conversation]
      when(conversationWithoutChannel.maybeChannel).thenReturn(None)

      Normal.channelToUseFor(originatingChannel, Some(conversationWithoutChannel), None, Some(dmChannel)) mustBe originatingChannel
      Threaded.channelToUseFor(originatingChannel, Some(conversationWithoutChannel), None, Some(dmChannel)) mustBe originatingChannel

      Normal.channelToUseFor(originatingChannel, None, None, Some(dmChannel)) mustBe originatingChannel
      Threaded.channelToUseFor(originatingChannel, None, None, Some(dmChannel)) mustBe originatingChannel
    }

    "Use the DM channel if present for Private response type, otherwise fallback" in {
      val conversationWithChannel = mock[Conversation]
      when(conversationWithChannel.maybeChannel).thenReturn(Some(conversationChannel))

      val conversationWithoutChannel = mock[Conversation]
      when(conversationWithoutChannel.maybeChannel).thenReturn(None)

      Private.channelToUseFor(originatingChannel, Some(conversationWithChannel), Some(threadTs), Some(dmChannel)) mustBe dmChannel
      Private.channelToUseFor(originatingChannel, None, Some(threadTs), Some(dmChannel)) mustBe dmChannel
      Private.channelToUseFor(originatingChannel, Some(conversationWithChannel), None, Some(dmChannel)) mustBe dmChannel

      Private.channelToUseFor(originatingChannel, Some(conversationWithChannel), Some(threadTs), None) mustBe originatingChannel
      Private.channelToUseFor(originatingChannel, Some(conversationWithChannel), None, None) mustBe conversationChannel
      Private.channelToUseFor(originatingChannel, Some(conversationWithoutChannel), None, None) mustBe originatingChannel
      Private.channelToUseFor(originatingChannel, None, None, None) mustBe originatingChannel
    }
  }

  "maybeThreadTsToUseFor" should {
    "return None if there is no thread to use and no triggering message" in {
      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, None) mustBe None
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, None) mustBe None
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, None) mustBe None
    }

    "return None if there is no thread to use unless threaded response and triggering message" in {
      val maybeMessageId = Some(SlackTimestamp.now)
      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, maybeMessageId) mustBe None
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, maybeMessageId) mustBe maybeMessageId
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None, maybeMessageId) mustBe None
    }

    "prioritize the conversation thread if present when not Private in another channel" in {
      val conversationWithThread = mock[Conversation]
      when(conversationWithThread.maybeThreadId).thenReturn(Some(conversationThreadTs))

      val maybeMessageId = Some(SlackTimestamp.now)
      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs), maybeMessageId) mustBe Some(conversationThreadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs), maybeMessageId) mustBe Some(conversationThreadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs), maybeMessageId) mustBe Some(conversationThreadTs)
    }

    "use the original thread if no conversation thread and not Private in another channel" in {
      val conversationWithoutThread = mock[Conversation]
      when(conversationWithoutThread.maybeThreadId).thenReturn(None)

      val maybeMessageId = Some(SlackTimestamp.now)

      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs), maybeMessageId) mustBe Some(threadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs), maybeMessageId) mustBe Some(threadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs), maybeMessageId) mustBe Some(threadTs)

      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs), maybeMessageId) mustBe Some(threadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs), maybeMessageId) mustBe Some(threadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs), maybeMessageId) mustBe Some(threadTs)
    }

    "return None if a Private response happens in another channel" in {
      val conversation = mock[Conversation]
      val maybeMessageId = Some(SlackTimestamp.now)
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, Some(conversation), Some(threadTs), maybeMessageId) mustBe None
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, None, Some(threadTs), maybeMessageId) mustBe None
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, Some(conversation), None, maybeMessageId) mustBe None
    }
  }

}
