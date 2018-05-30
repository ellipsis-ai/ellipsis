import models.accounts.slack.botprofile.SlackBotProfile
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSClient
import services.caching.CacheService
import utils.SlackChannels


class SlackChannelsSpec extends PlaySpec with MockitoSugar {

  val channels = SlackChannels(mock[SlackBotProfile], mock[CacheService], mock[WSClient])
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
