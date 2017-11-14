package controllers

import java.time.{LocalTime, OffsetDateTime, ZoneId}

import akka.actor.ActorSystem
import models.accounts.user.User
import models.accounts.user.UserTeamAccess
import models.team.Team
import org.mockito.Mockito.when
import org.mockito.Matchers
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test._
import support.ControllerTestContextWithLoggedInUser
import play.api.test.Helpers._
import services.{CacheService, DataService}
import com.mohiva.play.silhouette.test._
import json.{ScheduledActionData, ScheduledActionRecurrenceData, ScheduledActionRecurrenceTimeData}
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import slack.models.{Group, GroupValue}
import utils.{ChannelLike, SlackChannels, SlackGroup}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ScheduledActionsControllerSpec extends PlaySpec with MockitoSugar {
  val slackBotUserId = "B1234"
  val slackUserId = "U1234"
  val otherSlackUserId = "U5678"
  val slackTeamId = "T1234"
  val channel1Id = "G1234"
  val channel2Id = "G5678"
  val aTimestamp: Long = OffsetDateTime.now.minusYears(1).toEpochSecond
  val otherTeamId = IDs.next
  val otherTeam = Team(otherTeamId, "Other team", None, OffsetDateTime.now)

  def makeSlackGroup(id: String, name: String, includeUser: Boolean): ChannelLike = {
    val members: Seq[String] = Seq(slackBotUserId, otherSlackUserId) ++ (if (includeUser) {
      Seq(slackUserId)
    } else {
      Seq()
    })
    SlackGroup(
      Group(
        id,
        name,
        is_group = true,
        created = aTimestamp,
        creator = otherSlackUserId,
        is_archived = false,
        members = members,
        topic = GroupValue("some group topic", otherSlackUserId, aTimestamp),
        purpose = GroupValue("some group purpose", otherSlackUserId, aTimestamp),
        last_read = None,
        latest = None,
        unread_count = None,
        unread_count_display = None
      )
    )
  }

  def makeScheduleFor(channelId: String, team: Team): ScheduledMessage = {
    val recurrence = Daily(IDs.next, 1, LocalTime.NOON, ZoneId.of("America/Toronto"))
    ScheduledMessage(
      IDs.next,
      ":tada:",
      None,
      team = team,
      Some(channelId),
      isForIndividualMembers = false,
      recurrence,
      nextSentAt = OffsetDateTime.now.plusDays(1),
      createdAt = OffsetDateTime.now
    )
  }

  def setup(user: User, team: Team, dataService: DataService, cacheService: CacheService)
           (implicit actorSystem: ActorSystem, ec: ExecutionContext): Unit = {
    val normalTeamAccess = UserTeamAccess(user, team, Some(team), isAdminAccess = false)
    val adminTeamAccess = UserTeamAccess(user, team, Some(otherTeam), isAdminAccess = true)
    when(dataService.users.teamAccessFor(user, None)).thenReturn(Future.successful(normalTeamAccess))
    when(dataService.users.teamAccessFor(user, Some(otherTeamId))).thenReturn(Future.successful(adminTeamAccess))

    val slackBotProfile = SlackBotProfile(slackBotUserId, team.id, slackTeamId, "ABCD", OffsetDateTime.now)
    when(dataService.slackBotProfiles.allFor(team)).thenReturn(Future.successful(Seq(slackBotProfile)))

    when(dataService.linkedAccounts.maybeSlackUserIdFor(user)(ec)).thenReturn(Future.successful(Some(slackUserId)))

    val slackChannels = mock[SlackChannels]
    val channels = Seq(
      makeSlackGroup(channel1Id, "general", includeUser = true),
      makeSlackGroup(channel2Id, "other", includeUser = false)
    )
    when(dataService.slackBotProfiles.channelsFor(any[SlackBotProfile], any[CacheService])).thenReturn(slackChannels)
    when(slackChannels.getListForUser(any[Option[String]])(any[ActorSystem], any[ExecutionContext])).thenReturn(Future.successful(channels))

    val schedules = Seq(makeScheduleFor(channel1Id, team), makeScheduleFor(channel2Id, team))
    when(dataService.scheduledBehaviors.allActiveForTeam(team)).thenReturn(Future.successful(Seq()))
    when(dataService.scheduledMessages.allForTeam(team)).thenReturn(Future.successful(schedules))
    when(dataService.behaviorGroups.allFor(team)).thenReturn(Future.successful(Seq()))
  }

  "index" should {
    "return the list of scheduled actions a user can see in non-admin mode" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        actorSystem
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val request = FakeRequest(controllers.routes.ScheduledActionsController.index(None, None, None)).withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe OK
      }
    }
  }
}
