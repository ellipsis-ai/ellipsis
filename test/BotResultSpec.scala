import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.SuccessResult
import models.behaviors.events.SlackMessageEvent
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
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
  val defaultSlackToken = IDs.next
  val defaultThreadId: Option[String] = None

  implicit val system = ActorSystem("slack")

  def runNow[T](f: Future[T]) = Await.result(f, 30.seconds)

  "sendIn" should {

    "send a response" in new TestContext {
      val profile = SlackBotProfile("U3456789", team.id, defaultSlackTeamId, defaultSlackToken, OffsetDateTime.now)
      val event = SlackMessageEvent(profile, defaultChannel, defaultThreadId, defaultSlackUserId, "", SlackTimestamp.now)
      val responseText = "response"
      val result = SuccessResult(event, None, JsString("result"), Seq(), Some(responseText), None, forcePrivateResponse = false)
      val resultTs = SlackTimestamp.now

      val slackClient = mock[SlackApiClient]
      when(dataService.slackBotProfiles.clientFor(profile)).thenReturn(slackClient)
      when(slackClient.listIms).thenReturn(Future.successful(Seq()))
      when(dataService.conversations.allOngoingFor(defaultSlackUserId, event.context, event.maybeChannel, event.maybeThreadId)).thenReturn(Future.successful(Seq()))

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

      runNow(result.sendIn(None, None, dataService)) mustBe Some(resultTs)

    }

  }
}
