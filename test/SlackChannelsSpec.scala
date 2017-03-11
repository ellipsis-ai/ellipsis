import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import slack.api.SlackApiClient
import utils.SlackChannels


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

}
