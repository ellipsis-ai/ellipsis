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
import services.DataService
import services.caching.CacheService
import support.TestContext
import utils._

import scala.concurrent.{ExecutionContext, Future}

class ScheduledActionsConfigSpec extends PlaySpec with MockitoSugar {
  val slackBotUserId = "B1234"
  val slackUserId = "U1234"
  val otherSlackUserId = "U5678"
  val slackUserIdsWithoutUser = Seq(slackBotUserId, otherSlackUserId)
  val slackUserIdsWithUser = slackUserIdsWithoutUser ++ Seq(slackUserId)
  val slackTeamId = "T1234"
  val channel1Id = "G1234"
  val channel2Id = "G5678"
  val aTimestamp: Long = OffsetDateTime.now.minusYears(1).toEpochSecond
  val otherTeam = Team("Other team")
  val channels = Seq(
    makeSlackGroup(channel1Id, "general"),
    makeSlackGroup(channel2Id, "other")
  )
  val maybeCsrfToken = Some("nothing to see here")

  def makeSlackGroup(id: String, name: String): SlackConversation = {
    SlackConversation.defaultFor(id, name).copy(is_group = Some(true), is_private = Some(true))
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
    when(dataService.slackBotProfiles.channelsFor(any[SlackBotProfile])).thenReturn(slackChannels)
    when(slackChannels.getMembersFor(channel1Id)).thenReturn(Future.successful(slackUserIdsWithUser))
    when(slackChannels.getMembersFor(channel2Id)).thenReturn(Future.successful(slackUserIdsWithoutUser))
    when(slackChannels.getList).thenReturn(
      if (blowup) {
        Future.failed(SlackApiError("account_inactive"))
      } else {
        Future.successful(channels)
      }
    )
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
        val teamAccess = UserTeamAccess(user, otherTeam, None, None, isAdminAccess = false)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = false)(actorSystem, ec))
        maybeConfig mustEqual None
      }
    }

    "return the list of scheduled actions a user can see in non-admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = false)(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 1
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.channelList.get.length mustEqual 1
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return the whole list of scheduled actions in admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), Some("TestBot"), isAdminAccess = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = false)(actorSystem, ec))
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

    "return the whole list of scheduled actions in force admin mode" in new TestContext {
      running(app) {
        setup(user, team, dataService, cacheService)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = true)(actorSystem, ec))
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
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = false)(actorSystem, ec))
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
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), Some("TestBot"), isAdminAccess = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(user, teamAccess, services, None, None, maybeCsrfToken, forceAdmin = false)(actorSystem, ec))
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
