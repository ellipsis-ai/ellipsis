import java.time.{LocalTime, OffsetDateTime, ZoneId}

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.{ConversationMemberInfo, ScheduledActionsConfig}
import models.IDs
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.slack.apiModels.SlackTeam
import services.slack.{SlackApiClient, SlackApiError}
import services.{DataService, DefaultServices}
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
  val slackAdminTeamId = "TADMIN"
  val channel1Id = "G1234"
  val channel2Id = "G5678"
  val aTimestamp: Long = OffsetDateTime.now.minusYears(1).toEpochSecond
  val otherTeam = Team("Other team")
  val channels = Seq(
    makeSlackGroup(channel1Id, "general"),
    makeSlackGroup(channel2Id, "other")
  )
  val csrfToken = "nothing to see here"
  val dmId = "D1234"
  val dmName = "Direct message"
  val dmWithBot = SlackConversation.defaultFor(dmId, dmName)
    .copy(
      is_im = Some(true),
      // 2019/04/12: Slack API doesn't return any value for properties below for DM conversations
      is_channel = None,
      is_group = None,
      is_private = None,
      is_member = None,
      num_members = None
    )

  def makeSlackGroup(id: String, name: String): SlackConversation = {
    SlackConversation.defaultFor(id, name).copy(is_group = Some(true), is_private = Some(true))
  }

  def makeScheduleFor(channelId: String, team: Team): ScheduledMessage = {
    val recurrence = Daily(IDs.next, 1, 0, None, LocalTime.NOON, ZoneId.of("America/Toronto"))
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

  def setup(user: User, team: Team, services: DefaultServices, blowup: Boolean = false, userSlackTeamId: String = slackTeamId)
           (implicit actorSystem: ActorSystem, ec: ExecutionContext): Unit = {
    val slackBotProfile = SlackBotProfile(slackBotUserId, team.id, slackTeamId, "ABCD", OffsetDateTime.now, allowShortcutMention = true)
    val slackUserProfile = SlackProfile(SlackUserTeamIds(userSlackTeamId), LoginInfo(Conversation.SLACK_CONTEXT, slackUserId), None)
    when(services.dataService.users.maybeSlackProfileFor(user)).thenReturn(Future.successful(Some(slackUserProfile)))
    when(services.dataService.slackBotProfiles.allFor(team)).thenReturn(Future.successful(Seq(slackBotProfile)))
    when(services.dataService.linkedAccounts.maybeSlackUserIdFor(user)(ec)).thenReturn(Future.successful(Some(slackUserId)))

    val slackChannels = mock[SlackChannels]
    when(services.dataService.slackBotProfiles.channelsFor(any[SlackBotProfile])).thenReturn(slackChannels)
    when(slackChannels.getMembersFor(channel1Id)).thenReturn(Future.successful(slackUserIdsWithUser))
    when(slackChannels.getMembersFor(channel2Id)).thenReturn(Future.successful(slackUserIdsWithoutUser))
    when(slackChannels.getList).thenReturn(
      if (blowup) {
        Future.failed(SlackApiError("account_inactive"))
      } else {
        Future.successful(channels)
      }
    )
    when(slackChannels.botUserId).thenReturn(slackBotProfile.userId)
    val slackApiClient = mock[SlackApiClient]
    when(services.slackApiService.clientFor(slackBotProfile)).thenReturn(slackApiClient)
    when(slackApiClient.getTeamInfo).thenReturn(Future.successful(Some(SlackTeam (
      Some(slackTeamId),
      Some("Test team"),
      None,
      None,
      None,
      None
    ))))
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
        setup(user, team, services)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, None, None, isAdminAccess = false, isAdminUser = false)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = false
        )(actorSystem, ec))
        maybeConfig mustEqual None
      }
    }

    "return the list of scheduled actions a user can see in non-admin mode" in new TestContext {
      running(app) {
        setup(user, team, services)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false, isAdminUser = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = false
        )(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 1
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.orgChannels.teamChannels.head.channelList.length mustEqual 1
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return the whole list of scheduled actions in admin mode" in new TestContext {
      running(app) {
        setup(user, team, services, userSlackTeamId = slackAdminTeamId)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), Some("TestBot"), isAdminAccess = true, isAdminUser = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = false
        )(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 2
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.scheduledActions(1).id.get mustEqual schedules(1).id
          config.orgChannels.teamChannels.head.channelList.length mustEqual channels.length
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return the whole list of scheduled actions in force admin mode" in new TestContext {
      running(app) {
        setup(user, team, services)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false, isAdminUser = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = true
        )(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 2
          config.scheduledActions.head.id.get mustEqual schedules.head.id
          config.scheduledActions(1).id.get mustEqual schedules(1).id
          config.orgChannels.teamChannels.head.channelList.length mustEqual channels.length
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return no channels and no schedules if the slack API raises an exception" in new TestContext {
      running(app) {
        setup(user, team, services, blowup = true)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false, isAdminUser = false)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = false
        )(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 0
          config.orgChannels.teamChannels.length mustEqual 0
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

    "return no channels and all schedules if the slack API raises an exception in admin mode" in new TestContext {
      running(app) {
        setup(user, team, services, blowup = true)(actorSystem, ec)
        val teamAccess = UserTeamAccess(user, otherTeam, Some(team), Some("TestBot"), isAdminAccess = true, isAdminUser = true)
        val schedules = setupSchedules(team, dataService)
        val maybeConfig = await(ScheduledActionsConfig.buildConfigFor(
          user = user,
          teamAccess = teamAccess,
          services = services,
          maybeScheduledId = None,
          maybeNewSchedule = None,
          maybeFilterChannelId = None,
          maybeFilterBehaviorGroupId = None,
          csrfToken = csrfToken,
          forceAdmin = false
        )(actorSystem, ec))
        maybeConfig.map { config =>
          config.scheduledActions.length mustBe 2
          config.orgChannels.teamChannels.length mustEqual 0
        }.getOrElse {
          assert(false, "No config returned")
        }
      }
    }

  }

  "maybeChannelDataFor" should {
    val slackBotProfile = SlackBotProfile(slackBotUserId, "teamId", slackTeamId, "token", OffsetDateTime.now, true)
    val userSlackProfile = SlackProfile(SlackUserTeamIds(slackTeamId), LoginInfo("slack", slackUserId), None)

    "mark a conversation as isSelfDm if the user and bot are members of an IM" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(convoId = dmId)).thenReturn(Future.successful(Seq(slackUserId, slackBotUserId)))
      val maybeChannelData = await(ScheduledActionsConfig.maybeChannelDataFor(dmWithBot, slackBotProfile, Some(userSlackProfile), slackChannels, isAdmin = false))
      maybeChannelData.exists(_.isSelfDm) mustEqual true
      maybeChannelData.exists(_.isOtherDm) mustEqual false
    }

    "mark a conversation as isOtherDm if the user is not a member but is an admin" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(convoId = dmId)).thenReturn(Future.successful(Seq(otherSlackUserId, slackBotUserId)))
      val maybeChannelData = await(ScheduledActionsConfig.maybeChannelDataFor(dmWithBot, slackBotProfile, Some(userSlackProfile), slackChannels, isAdmin = true))
      maybeChannelData.exists(_.isSelfDm) mustEqual false
      maybeChannelData.exists(_.isOtherDm) mustEqual true
    }

    "mark a conversation as isOtherDm if the user is not on the team but is an admin" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(convoId = dmId)).thenReturn(Future.successful(Seq(otherSlackUserId, slackBotUserId)))
      val maybeChannelData = await(ScheduledActionsConfig.maybeChannelDataFor(dmWithBot, slackBotProfile, None, slackChannels, isAdmin = true))
      maybeChannelData.exists(_.isSelfDm) mustEqual false
      maybeChannelData.exists(_.isOtherDm) mustEqual true
    }

    "mark a conversation as neither kind of DM if it's a private group channel" in new TestContext {
      val group = makeSlackGroup(channel1Id, "private")
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(channel1Id)).thenReturn(Future.successful(Seq(slackUserId, slackBotUserId)))
      val maybeChannelData = await(ScheduledActionsConfig.maybeChannelDataFor(group, slackBotProfile, Some(userSlackProfile), slackChannels, isAdmin = false))
      maybeChannelData.exists(_.isSelfDm) mustEqual false
      maybeChannelData.exists(_.isOtherDm) mustEqual false
    }

    "return None if the user is not a member of an IM and is not an admin" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(convoId = dmId)).thenReturn(Future.successful(Seq(otherSlackUserId, slackBotUserId)))
      val maybeChannelData = await(ScheduledActionsConfig.maybeChannelDataFor(dmWithBot, slackBotProfile, Some(userSlackProfile), slackChannels, isAdmin = false))
      maybeChannelData mustBe None
    }

  }

  "maybePrivateMemberInfo" should {
    val slackBotProfile = SlackBotProfile(slackBotUserId, "teamId", slackTeamId, IDs.next, OffsetDateTime.now, allowShortcutMention = true)
    val userSlackProfile = SlackProfile(SlackUserTeamIds(slackTeamId), LoginInfo("slack", slackUserId), None)

    "return isUserMember = false if the user is not a member of a private conversation" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(dmId)).thenReturn(Future.successful(Seq(otherSlackUserId, slackBotUserId)))
      await(ScheduledActionsConfig.maybePrivateMemberInfo(dmWithBot, slackBotProfile, Some(userSlackProfile), slackChannels)) mustEqual Some(ConversationMemberInfo(false, true))
    }

    "return isUserMember = true if the user is a member of a private conversation" in new TestContext {
      val slackChannels = mock[SlackChannels]
      when(slackChannels.getMembersFor(dmId)).thenReturn(Future.successful(Seq(slackUserId, slackBotUserId)))
      await(ScheduledActionsConfig.maybePrivateMemberInfo(dmWithBot, slackBotProfile, Some(userSlackProfile), slackChannels)) mustEqual Some(ConversationMemberInfo(true, true))
    }

    "return None if the conversation is not private" in new TestContext {
      val slackChannels = mock[SlackChannels]
      val channel = SlackConversation.defaultFor(channel1Id, "Channel").copy(is_channel = Some(true), is_member = Some(true))
      await(ScheduledActionsConfig.maybePrivateMemberInfo(channel, slackBotProfile, Some(userSlackProfile), slackChannels)) mustEqual None
    }
  }
}
