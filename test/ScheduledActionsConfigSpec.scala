import java.time.{LocalTime, OffsetDateTime, ZoneId}

import akka.actor.ActorSystem
import json.ScheduledActionsConfig
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.{CacheService, DataService}
import slack.models.{Group, GroupValue}
import support.TestContext
import utils.{ChannelLike, SlackChannels, SlackGroup}

import scala.concurrent.{ExecutionContext, Future}

class ScheduledActionsConfigSpec extends PlaySpec with MockitoSugar {
  val slackBotUserId = "B1234"
  val slackUserId = "U1234"
  val otherSlackUserId = "U5678"
  val slackTeamId = "T1234"
  val channel1Id = "G1234"
  val channel2Id = "G5678"
  val aTimestamp: Long = OffsetDateTime.now.minusYears(1).toEpochSecond
  val otherTeamId = IDs.next
  val otherTeam = Team(otherTeamId, "Other team", None, OffsetDateTime.now)
  val channels = Seq(
    makeSlackGroup(channel1Id, "general", includeUser = true),
    makeSlackGroup(channel2Id, "other", includeUser = false)
  )
  val maybeCsrfToken = Some("nothing to see here")

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

  def setup(user: User, team: Team, dataService: DataService, cacheService: CacheService, blowup: Boolean = false)
           (implicit actorSystem: ActorSystem, ec: ExecutionContext): Unit = {
    val slackBotProfile = SlackBotProfile(slackBotUserId, team.id, slackTeamId, "ABCD", OffsetDateTime.now)
    when(dataService.slackBotProfiles.allFor(team)).thenReturn(Future.successful(Seq(slackBotProfile)))
    when(dataService.linkedAccounts.maybeSlackUserIdFor(user)(ec)).thenReturn(Future.successful(Some(slackUserId)))

    val slackChannels = mock[SlackChannels]
    when(dataService.slackBotProfiles.channelsFor(any[SlackBotProfile], any[CacheService])).thenReturn(slackChannels)
    when(slackChannels.getListForUser(any[Option[String]])(any[ActorSystem], any[ExecutionContext]))
      .thenReturn {
        if (blowup) {
          Future.failed(slack.api.ApiError("account_inactive"))
        } else {
          Future.successful(channels)
        }
      }
  }

  def setupSchedules(team: Team, dataService: DataService): Seq[ScheduledMessage] = {
    val schedules = Seq(makeScheduleFor(channel1Id, team), makeScheduleFor(channel2Id, team))
    when(dataService.scheduledBehaviors.allActiveForTeam(team)).thenReturn(Future.successful(Seq()))
    when(dataService.scheduledMessages.allForTeam(team)).thenReturn(Future.successful(schedules))
    when(dataService.behaviorGroups.allFor(team)).thenReturn(Future.successful(Seq()))
    schedules
  }

  "buildFor" should {
    "return no config for a user with no access" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, None, isAdminAccess = false)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken)(actorSystem, ec))
        maybeConfig mustEqual None
      }
    }

    "return the list of scheduled actions a user can see in non-admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), isAdminAccess = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken)(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 1
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.channelList.get.length mustEqual channels.length
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return the whole list of scheduled actions in admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), isAdminAccess = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken)(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 2
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.scheduledActions(1).id.get mustEqual schedules(1).id
          config.channelList.get.length mustEqual channels.length
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return no channels and no schedules if the slack API raises an exception" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService, blowup = true)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), isAdminAccess = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken)(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 0
          config.channelList mustEqual None
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return no channels and all schedules if the slack API raises an exception in admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService, blowup = true)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), isAdminAccess = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken)(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 2
          config.channelList mustEqual None
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

  }
}