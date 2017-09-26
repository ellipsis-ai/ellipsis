package mocks

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.SlackUserData
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackMessageEvent
import org.mockito.Matchers._
import org.mockito.Mockito._
import services.SlackEventService
import slack.api.SlackApiClient
import org.scalatest.mock.MockitoSugar
import slack.models.Attachment
import utils.SlackTimestamp

import scala.concurrent.Future

class MockSlackEventService extends SlackEventService with MockitoSugar {

  val client: SlackApiClient = {
    val client = mock[SlackApiClient]
    implicit val system = ActorSystem("slack")
    when(client.listIms()(system)).thenReturn(Future.successful(Seq()))
    when(client.postChatMessage(
      anyString,
      anyString,
      any[Option[String]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[String]],
      any[Option[Seq[Attachment]]],
      any[Option[Boolean]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[String]],
      any[Option[Boolean]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[Boolean]])(any[ActorSystem])
    ).thenReturn(Future.successful(SlackTimestamp.now))
    when(client.listIms).thenReturn(Future.successful(Seq()))
    val slackUser = slack.models.User(
      id = IDs.next,
      name = IDs.next,
      deleted = None,
      color = None,
      profile = None,
      is_bot = None,
      is_admin = None,
      is_owner = None,
      is_primary_owner = None,
      is_restricted = None,
      is_ultra_restricted = None,
      has_2fa = None,
      has_files = None,
      tz = None,
      tz_offset = None,
      presence = None
    )
    val channel = slack.models.Channel(
      id = IDs.next,
      name = IDs.next,
      created = (OffsetDateTime.now.minusDays(365).toInstant.toEpochMilli),
      creator = slackUser.id,
      is_archived = None,
      is_member = None,
      is_general = None,
      is_channel = Some(true),
      is_group = None,
      is_mpim = None,
      num_members = None,
      members = None,
      topic = None,
      purpose = None,
      last_read = None,
      latest = None,
      unread_count = None,
      unread_count_display = None
    )
    when(client.getChannelInfo(anyString)(any[ActorSystem]())).thenReturn(Future.successful(channel))
    client
  }

  def onEvent(event: SlackMessageEvent): Future[Unit] = Future.successful({})

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = client

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]] = {
    Future.successful(Set())
  }

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

}
