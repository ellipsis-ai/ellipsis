import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.slack.SlackApiClient
import support.TestContext
import utils.{SlackChannels, SlackConversation}

import scala.concurrent.Future

class SlackChannelsSpec extends PlaySpec with MockitoSugar {

  val channels = SlackChannels(mock[SlackApiClient])
  val channelId = "C123456"

  "unformatChannelText" should {

    "unformat slack-formatted channels correctly" in  {
      channels.unformatChannelText(s"<#$channelId|some_channel>") mustBe channelId
    }

    "unformat away extra hash mark" in {
      channels.unformatChannelText(s"#$channelId") mustBe channelId

    }

    "no-op when no slack formatting" in {
      channels.unformatChannelText(channelId) mustBe channelId
    }

  }

  "maybeIdFor" should {

    val slackConversation1 = SlackConversation.defaultFor(channelId, "test-channel")
    val slackConversation2 = SlackConversation.defaultFor("C654321", "other-channel")
    val allConversations = Seq(slackConversation1, slackConversation2)

    "return the ID if getInfo finds the conversation with or without the channel formatting junk" in new TestContext {
      val formattedChannel = s"<#${slackConversation1.id}|test-channel>"
      when(channels.client.conversationInfo(slackConversation1.id)).thenReturn(Future.successful(Some(slackConversation1)))
      when(channels.client.conversationInfo(slackConversation2.id)).thenReturn(Future.successful(Some(slackConversation2)))
      await(channels.maybeIdFor(slackConversation1.id)) mustBe Some(slackConversation1.id)
      await(channels.maybeIdFor(slackConversation2.id)) mustBe Some(slackConversation2.id)
      await(channels.maybeIdFor(formattedChannel)) mustBe Some(slackConversation1.id)
    }

    "map a name to an ID if there's a same-name item in getList, with or without the # prefix" in new TestContext {
      when(channels.client.conversationInfo(any[String])).thenReturn(Future.successful(None))
      when(channels.client.listConversations()).thenReturn(Future.successful(allConversations))
      await(channels.maybeIdFor("test-channel")) mustBe Some(slackConversation1.id)
      await(channels.maybeIdFor("#test-channel")) mustBe Some(slackConversation1.id)
      await(channels.maybeIdFor("other-channel")) mustBe Some(slackConversation2.id)
    }

    "return none for non-matching IDs" in new TestContext {
      when(channels.client.conversationInfo(any[String])).thenReturn(Future.successful(None))
      when(channels.client.listConversations()).thenReturn(Future.successful(allConversations))
      await(channels.maybeIdFor("non-existing")) mustBe None
      await(channels.maybeIdFor("G1234")) mustBe None
    }

  }

}
