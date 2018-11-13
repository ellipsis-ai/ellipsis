import java.time.OffsetDateTime

import json.SlackUserData
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackEventContext
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.DefaultServices
import support.TestContext

import scala.concurrent.Future

class SlackMessageEventSpec extends PlaySpec with MockitoSugar {
  val botToken = IDs.next
  val botUserId = IDs.next
  val botTeamId = IDs.next
  val slackTeamId = IDs.next
  val botName = "MockieBot"

  def newEvent(channel: String, services: DefaultServices, maybeBotName: Option[String] = Some(botName)): SlackMessageEvent = {
    val profile = SlackBotProfile(botUserId, botTeamId, slackTeamId, botToken, OffsetDateTime.now, allowShortcutMention = true)
    when(services.dataService.slackBotProfiles.maybeNameFor(profile)).thenReturn(Future.successful(maybeBotName))
    SlackMessageEvent(
      SlackEventContext(
        profile,
        channel,
        None,
        IDs.next
      ),
      SlackMessage("oh hai", "oh hai", "oh hai", Set.empty[SlackUserData]),
      None,
      OffsetDateTime.now.toString,
      None,
      isUninterruptedConversation = false,
      isEphemeral = false,
      maybeResponseUrl = None,
      beQuiet = false
    )
  }

  "botName" should {
    "return the bot display name" in new TestContext {
      running(app) {
        val event = newEvent("C1234", services)
        val maybeInfo = await(event.maybeBotInfo(services)(actorSystem, ec))
        maybeInfo.map(_.name) must contain(botName)
      }
    }
  }

  "contextualBotPrefix" should {
    "return the mention-bot prefix in a normal channel" in new TestContext {
      running(app) {
        val event = newEvent("C1234", services)
        val prefix = await(event.contextualBotPrefix(services)(actorSystem, ec))
        prefix mustBe s"@$botName "
      }
    }

    "return empty prefix in a direct message" in new TestContext {
      running(app) {
        val event = newEvent("D1234", services)
        val prefix = await(event.contextualBotPrefix(services)(actorSystem, ec))
        prefix mustBe ""
      }
    }

    "return the fallback prefix when there is no name available" in new TestContext {
      running(app) {
        val event = newEvent("C1234", services, None)
        val prefix = await(event.contextualBotPrefix(services)(actorSystem, ec))
        prefix mustBe SlackMessageEvent.fallbackBotPrefix
      }
    }
  }
}
