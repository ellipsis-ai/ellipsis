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
    "return None if there is no thread to use" in {
      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None) mustBe None
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None) mustBe None
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, None) mustBe None
    }

    "prioritize the conversation thread if present when not Private in another channel" in {
      val conversationWithThread = mock[Conversation]
      when(conversationWithThread.maybeThreadId).thenReturn(Some(conversationThreadTs))

      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs)) mustBe Some(conversationThreadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs)) mustBe Some(conversationThreadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithThread), Some(threadTs)) mustBe Some(conversationThreadTs)
    }

    "use the original thread if no conversation thread and not Private in another channel" in {
      val conversationWithoutThread = mock[Conversation]
      when(conversationWithoutThread.maybeThreadId).thenReturn(None)

      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs)) mustBe Some(threadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs)) mustBe Some(threadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, Some(conversationWithoutThread), Some(threadTs)) mustBe Some(threadTs)

      Normal.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs)) mustBe Some(threadTs)
      Threaded.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs)) mustBe Some(threadTs)
      Private.maybeThreadTsToUseFor(originatingChannel, originatingChannel, None, Some(threadTs)) mustBe Some(threadTs)
    }

    "return None if a Private response happens in another channel" in {
      val conversation = mock[Conversation]
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, Some(conversation), Some(threadTs)) mustBe None
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, None, Some(threadTs)) mustBe None
      Private.maybeThreadTsToUseFor(dmChannelToUse, originatingChannel, Some(conversation), None) mustBe None
    }
  }

  "maybeThreadTsToUseForNextAction" should {
    "use the original message TS for Threaded responses with no current thread" in {
      val event = mock[Event]
      when(event.maybeChannel).thenReturn(Some(originatingChannel))
      when(event.maybeThreadId).thenReturn(None)

      val conversationWithoutThread = mock[Conversation]
      when(conversationWithoutThread.maybeThreadId).thenReturn(None)

      Threaded.maybeThreadTsToUseForNextAction(botResultFor(event, Some(conversationWithoutThread)), originatingChannel, Some(originalMessageTs)) mustBe Some(originalMessageTs)
      Threaded.maybeThreadTsToUseForNextAction(botResultFor(event, None), originatingChannel, Some(originalMessageTs)) mustBe Some(originalMessageTs)
    }

    "otherwise use the existing maybeThreadTsToUseFor logic for Threaded responses" in {
      val event = mock[Event]
      when(event.maybeChannel).thenReturn(Some(originatingChannel))
      when(event.maybeThreadId).thenReturn(Some(threadTs))

      val conversationWithThread = mock[Conversation]
      when(conversationWithThread.maybeThreadId).thenReturn(Some(conversationThreadTs))

      Threaded.maybeThreadTsToUseForNextAction(botResultFor(event, Some(conversationWithThread)), originatingChannel, Some(originalMessageTs)) mustBe Some(conversationThreadTs)
      Threaded.maybeThreadTsToUseForNextAction(botResultFor(event, None), originatingChannel, Some(originalMessageTs)) mustBe Some(threadTs)
    }

    "use maybeThreadToUseFor for Normal or Private responses" in {
      val eventWithThread = mock[Event]
      when(eventWithThread.maybeChannel).thenReturn(Some(originatingChannel))
      when(eventWithThread.maybeThreadId).thenReturn(Some(threadTs))

      val eventWithoutThread = mock[Event]
      when(eventWithoutThread.maybeChannel).thenReturn(Some(originatingChannel))
      when(eventWithoutThread.maybeThreadId).thenReturn(None)

      val conversationWithThread = mock[Conversation]
      when(conversationWithThread.maybeThreadId).thenReturn(Some(conversationThreadTs))

      Normal.maybeThreadTsToUseForNextAction(botResultFor(eventWithoutThread, Some(conversationWithThread)), originatingChannel, Some(originalMessageTs)) mustBe Some(conversationThreadTs)
      Normal.maybeThreadTsToUseForNextAction(botResultFor(eventWithThread, None), originatingChannel, Some(originalMessageTs)) mustBe Some(threadTs)
      Normal.maybeThreadTsToUseForNextAction(botResultFor(eventWithoutThread, None), originatingChannel, Some(originalMessageTs)) mustBe None

      Private.maybeThreadTsToUseForNextAction(botResultFor(eventWithoutThread, Some(conversationWithThread)), originatingChannel, Some(originalMessageTs)) mustBe Some(conversationThreadTs)
      Private.maybeThreadTsToUseForNextAction(botResultFor(eventWithThread, None), originatingChannel, Some(originalMessageTs)) mustBe Some(threadTs)
      Private.maybeThreadTsToUseForNextAction(botResultFor(eventWithoutThread, None), originatingChannel, Some(originalMessageTs)) mustBe None

      Private.maybeThreadTsToUseForNextAction(botResultFor(eventWithThread, Some(conversationWithThread)), dmChannelToUse, Some(originalMessageTs)) mustBe None
    }
  }
}
