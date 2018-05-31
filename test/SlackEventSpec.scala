import java.time.OffsetDateTime

import json.{SlackUserData, SlackUserProfileData}
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackEvent
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import slack.api.SlackApiClient
import support.TestContext
import utils.SlackConversation

import scala.concurrent.Future

class SlackEventSpec extends PlaySpec with MockitoSugar {

  val slackTeamId = "T12345678"

  case class TestSlackEvent(
                             user: String,
                             channel: String,
                             client: SlackApiClient,
                             profile: SlackBotProfile
                           ) extends SlackEvent {

    val userSlackTeamId: String = slackTeamId
    val isUninterruptedConversation: Boolean = false
  }

  val slackUserId = "U12345678"
  val otherSlackUserId = "U87654321"
  val username = "lumbergh"
  val displayName = "Mr. Lumbergh"
  val firstName = "Bill"
  val lastName = "Lumbergh"
  val fullName = "Bill Lumbergh"
  val email = "billsporsche@company.com"
  val phone = "647-123-4567"
  val tz = "America/New_York"
  val profileData = SlackUserProfileData(Some(displayName), Some(firstName), Some(lastName), Some(fullName), Some(email), Some(phone))
  val slackUserData = SlackUserData(
    slackUserId,
    slackTeamId,
    username,
    isPrimaryOwner = false,
    isOwner = false,
    isRestricted = false,
    isUltraRestricted = false,
    isBot = false,
    Some(tz),
    deleted = false,
    Some(profileData)
  )

  val date = OffsetDateTime.now.minusDays(365).toInstant.toEpochMilli

  val slackConversation = SlackConversation.defaultFor(
    id = "C1000000",
    name = "The Channel"
  ).copy(members = Some(Array(slackUserId, otherSlackUserId)))

  val ellipsisTeamId = IDs.next
  val slackBotProfile = SlackBotProfile("U55555555", ellipsisTeamId, slackTeamId, IDs.next, OffsetDateTime.now)

  "detailsFor" should {
    "preserve the legacy format of the Slack user and channel details" in new TestContext {
      running(app) {

        val mockSlackClient = mock[SlackApiClient]

        when(services.slackEventService.maybeSlackUserDataFor(org.mockito.Matchers.eq[String](slackUserData.accountId), org.mockito.Matchers.eq[String](slackTeamId), any[SlackApiClient], any())).thenReturn(
          Future.successful(Some(slackUserData))
        )

        when(services.slackApiService.listConversations(slackBotProfile)).thenReturn(Future.successful(Seq(slackConversation)))

        when(services.slackApiService.conversationInfo(slackBotProfile, slackConversation.id)).thenReturn(Future.successful(Some(slackConversation)))

        val event = TestSlackEvent(slackUserData.accountId, slackConversation.id, mockSlackClient, slackBotProfile)
        val d: JsObject = await(event.detailsFor(services)(actorSystem, ec))
        (d \ "name").as[String] mustBe displayName
        (d \ "isPrimaryOwner").as[Boolean] mustBe slackUserData.isPrimaryOwner
        (d \ "isOwner").as[Boolean] mustBe slackUserData.isOwner
        (d \ "isRestricted").as[Boolean] mustBe slackUserData.isRestricted
        (d \ "isUltraRestricted").as[Boolean] mustBe slackUserData.isUltraRestricted
        (d \ "tz").as[String] mustBe slackUserData.tz.get
        (d \ "channelMembers").as[Seq[String]] mustBe Seq(slackUserId, otherSlackUserId)
        (d \ "channelName").as[String] mustBe slackConversation.name
        (d \ "profile" \ "firstName").as[String] mustBe firstName
        (d \ "profile" \ "lastName").as[String] mustBe lastName
        (d \ "profile" \ "realName").as[String] mustBe fullName
      }
    }
  }
}
