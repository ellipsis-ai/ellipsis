import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
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

    val slackConversation = SlackConversation.defaultFor(channelId, "test-channel")

    "return the ID if getInfo finds the conversation with or without the channel formatting junk" in new TestContext {
      when(channels.client.conversationInfo(channelId)).thenReturn(Future.successful(Some(slackConversation)))
      await(channels.maybeIdFor(channelId)) mustBe Some(channelId)
      await(channels.maybeIdFor(s"<#${slackConversation.id}|test-channel>")) mustBe Some(channelId)
    }

    "map a name to an ID if there's a same-name item in getList, with or without the # prefix and formatting junk" in new TestContext {
      when(channels.client.conversationInfo(channelId)).thenReturn(Future.successful(None))
      when(channels.client.listConversations()).thenReturn(Future.successful(Seq(slackConversation)))
      await(channels.maybeIdFor("test-channel")) mustBe Some(channelId)
      await(channels.maybeIdFor("#test-channel")) mustBe Some(channelId)
    }
  }

}
