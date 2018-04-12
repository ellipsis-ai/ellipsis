import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.{SlackUserData, SlackUserProfileData}
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.{ActionChoice, BehaviorResponse, DeveloperContext}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, EventType, MessageAttachmentGroup, SlackEvent}
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import services.{DataService, DefaultServices}
import slack.api.SlackApiClient
import slack.models.Channel
import support.TestContext
import utils.{SlackChannel, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}

class SlackEventSpec extends PlaySpec with MockitoSugar {

  val slackTeamId = "T12345678"

  case class TestSlackEvent(
                             user: String,
                             channel: String,
                             client: SlackApiClient,
                             profile: SlackBotProfile
                           ) extends SlackEvent {

    val userSlackTeamId: String = slackTeamId
    def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = Future.successful(Seq())

    def sendMessage(
                     text: String,
                     forcePrivate: Boolean,
                     maybeShouldUnfurl: Option[Boolean],
                     maybeConversation: Option[Conversation],
                     attachmentGroups: Seq[MessageAttachmentGroup],
                     files: Seq[UploadFileSpec],
                     choices: Seq[ActionChoice],
                     developerContext: DeveloperContext,
                     services: DefaultServices,
                     configuration: Configuration
                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = Future.successful(None)

    def withOriginalEventType(originalEventType: EventType, isUninterruptedConversation: Boolean): Event = this

    def allBehaviorResponsesFor(
                                 maybeTeam: Option[Team],
                                 maybeLimitToBehavior: Option[Behavior],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = Future.successful(Seq())

    val userIdForContext: String = user
    val invocationLogText: String = ""
    val messageText: String = ""
    val eventType: EventType = EventType.test
    val maybeThreadId: Option[String] = None
    val maybeChannel: Option[String] = Some(channel)
    val name: String = "test"
    val isResponseExpected: Boolean = false
    val maybeOriginalEventType: Option[EventType] = None
    val includesBotMention: Boolean = false
    val teamId: String = "T1"
  }

  val slackUserId = "U12345678"
  val otherSlackUserId = "U87654321"
  val username = "lumbergh"
  val displayName = "Mr. Lumbergh"
  val firstName = "Bill"
  val lastName = "Lumbergh"
  val fullName = "Bill Lumbergh"
  val tz = "America/New_York"
  val profileData = SlackUserProfileData(Some(displayName), Some(firstName), Some(lastName), Some(fullName))
  val slackUserData = SlackUserData(
    slackUserId,
    slackTeamId,
    username,
    isPrimaryOwner = false,
    isOwner = false,
    isRestricted = false,
    isUltraRestricted = false,
    Some(tz),
    deleted = false,
    Some(profileData)
  )

  val date = OffsetDateTime.now.minusDays(365).toInstant.toEpochMilli

  val channel = Channel(
    id = "C1000000",
    name = "The Channel",
    created = date,
    creator = slackUserId,
    is_archived = None,
    is_member = None,
    is_general = None,
    is_channel = Some(true),
    is_group = None,
    is_mpim = None,
    num_members = None,
    members = Some(Seq(slackUserId, otherSlackUserId)),
    topic = None,
    purpose = None,
    last_read = None,
    latest = None,
    unread_count = None,
    unread_count_display = None
  )
  val slackChannelInfo = SlackChannel(channel)

  val ellipsisTeamId = IDs.next
  val slackBotProfile = SlackBotProfile("U55555555", ellipsisTeamId, slackTeamId, IDs.next, OffsetDateTime.now)

  "detailsFor" should {
    "preserve the legacy format of the Slack user and channel details" in new TestContext {
      running(app) {

        val mockSlackClient = mock[SlackApiClient]

        when(services.slackEventService.maybeSlackUserDataFor(org.mockito.Matchers.eq[String](slackUserData.accountId), org.mockito.Matchers.eq[String](slackTeamId), any[SlackApiClient], any())).thenReturn(
          Future.successful(Some(slackUserData))
        )

        when(mockSlackClient.listIms()(actorSystem)).thenReturn(
          Future.successful(Seq())
        )

        when(mockSlackClient.getChannelInfo(slackChannelInfo.id)(actorSystem)).thenReturn(
          Future.successful(channel)
        )

        val event = TestSlackEvent(slackUserData.accountId, slackChannelInfo.id, mockSlackClient, slackBotProfile)
        val d: JsObject = await(event.detailsFor(services)(actorSystem, ec))
        (d \ "name").as[String] mustBe displayName
        (d \ "isPrimaryOwner").as[Boolean] mustBe slackUserData.isPrimaryOwner
        (d \ "isOwner").as[Boolean] mustBe slackUserData.isOwner
        (d \ "isRestricted").as[Boolean] mustBe slackUserData.isRestricted
        (d \ "isUltraRestricted").as[Boolean] mustBe slackUserData.isUltraRestricted
        (d \ "tz").as[String] mustBe slackUserData.tz.get
        (d \ "channelMembers").as[Seq[String]] mustBe Seq(slackUserId, otherSlackUserId)
        (d \ "channelName").as[String] mustBe slackChannelInfo.name
        (d \ "profile" \ "firstName").as[String] mustBe firstName
        (d \ "profile" \ "lastName").as[String] mustBe lastName
        (d \ "profile" \ "realName").as[String] mustBe fullName
      }
    }
  }
}
