import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.EventHandler
import models.behaviors.scheduling.recurrence.{Minutely, Recurrence}
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import support.TestContext

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class ScheduledBehaviorSpec extends PlaySpec with MockitoSugar {

  def runNow[T](f: Future[T]) = Await.result(f, 30.seconds)

  def mockNames(
                 dataService: DataService,
                 behavior: Behavior,
                 maybeBehaviorName: Option[String],
                 maybeBehaviorGroupName: Option[String]
               ) = {
    val behaviorVersion = mock[BehaviorVersion]
    when(behaviorVersion.maybeName).thenReturn(maybeBehaviorName)
    val behaviorGroupVersion = mock[BehaviorGroupVersion]
    when(behaviorGroupVersion.name).thenReturn(maybeBehaviorGroupName.getOrElse(""))
    when(dataService.behaviorGroups.maybeCurrentVersionFor(behavior.group))
      .thenReturn(Future.successful(Some(behaviorGroupVersion)))
    when(dataService.behaviors.maybeCurrentVersionFor(behavior)).thenReturn(Future.successful(Some(behaviorVersion)))
  }

  def newBehavior(team: Team): Behavior = {
    val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
    Behavior(IDs.next, team, Some(group), None, false, OffsetDateTime.now)
  }

  def newScheduledBehavior(
                            user: User,
                            behavior: Behavior,
                            team: Team,
                            channel: String = "C12345678",
                            isForIndividualMembers: Boolean = false,
                            recurrence: Recurrence = mock[Recurrence]
                          ): ScheduledBehavior = {
    ScheduledBehavior(
      id = IDs.next,
      behavior = behavior,
      arguments = Map(),
      maybeUser = Some(user),
      team = team,
      maybeChannel = Some(channel),
      isForIndividualMembers = isForIndividualMembers,
      recurrence,
      nextSentAt = OffsetDateTime.now,
      createdAt = OffsetDateTime.now
    )
  }

  "displayText" should {
    "include the behavior name and behavior group name when both exist" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        mockNames(dataService, behavior, Some("foo"), Some("bar"))
        val sb = newScheduledBehavior(user, behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an action named `foo` in skill `bar`"""
      }
    }

    "say an unnamed skill if no group name" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        mockNames(dataService, behavior, Some("foo"), None)
        val sb = newScheduledBehavior(user, behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an action named `foo` in an unnamed skill"""
      }
    }

    "say an unnamed action if there’s no action name" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        mockNames(dataService, behavior, None, None)
        val sb = newScheduledBehavior(user, behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an unnamed action in an unnamed skill"""
      }
    }

    "say an unnamed action with the skill name" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        mockNames(dataService, behavior, None, Some("bar"))
        val sb = newScheduledBehavior(user, behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an unnamed action in skill `bar`"""
      }
    }

    "say a deleted action/skill if there is none" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        when(dataService.behaviorGroups.maybeCurrentVersionFor(behavior.group)).thenReturn(Future.successful(None))
        when(dataService.behaviors.maybeCurrentVersionFor(behavior)).thenReturn(Future.successful(None))
        val sb = newScheduledBehavior(user, behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """a deleted action in a deleted skill"""
      }
    }
  }

  def scheduledBehaviorSpy(user: User, team: Team, channel: String, isForIndividualMembers: Boolean = false): ScheduledBehavior = {
    val behavior = newBehavior(team)
    val sb = newScheduledBehavior(user, behavior, team, channel, isForIndividualMembers = isForIndividualMembers)
    val sbSpy = spy(sb)
    doReturn(Future.successful(Unit)).when(sbSpy).
      sendFor(anyString, anyString, any[EventHandler], any[SlackBotProfile], any[DefaultServices])(
        any[ActorSystem],
        any[ExecutionContext]
      )
    doReturn(Future.successful(Unit)).when(sbSpy).
      sendForIndividualMembers(
        anyString,
        any[EventHandler],
        any[SlackBotProfile],
        any[DefaultServices]
      )(any[ActorSystem], any[ExecutionContext])
    sbSpy
  }

  "send" should {
    "call sendFor when there is a Slack profile and it’s not for individual members" in new TestContext {
      running(app) {
        val channel = "C12345678"
        val token = IDs.next
        val slackTeamId = "T1234567"
        val botProfile = SlackBotProfile("UMOCKBOT", team.id, None, slackTeamId, token, OffsetDateTime.now, allowShortcutMention = true)
        val userSlackId = "U1000"
        val userSlackProfile = SlackProfile(slackTeamId, LoginInfo("slack", userSlackId), None)
        when(services.dataService.users.maybeSlackProfileFor(user))
          .thenReturn(Future.successful(Some(userSlackProfile)))
        val sbSpy = scheduledBehaviorSpy(user, team, channel)
        val sent = sbSpy.send(eventHandler, botProfile, services, "Mock schedule")
        runNow(sent)
        verify(sbSpy, times(1)).sendFor(channel, userSlackId, eventHandler, botProfile, services)
        verify(sbSpy, times(0)).sendForIndividualMembers(
          anyString,
          any[EventHandler],
          any[SlackBotProfile],
          any[DefaultServices]
        )(any[ActorSystem], any[ExecutionContext])
      }
    }

    "call sendFor with the bot profile when there is no user Slack profile on a public channel not for individual members" in new TestContext {
      running(app) {
        val channel = "C12345678"
        val token = IDs.next
        val slackTeamId = "T1234567"
        val botProfile = SlackBotProfile("UMOCKBOT", team.id, None, slackTeamId, token, OffsetDateTime.now, allowShortcutMention = true)
        when(services.dataService.users.maybeSlackProfileFor(user)).thenReturn(Future.successful(None))
        val sbSpy = scheduledBehaviorSpy(user, team, channel)
        val sent = sbSpy.send(eventHandler, botProfile, services, "Mock schedule")
        runNow(sent)
        verify(sbSpy, times(1)).sendFor(channel, botProfile.userId, eventHandler, botProfile, services)
        verify(sbSpy, times(0)).sendForIndividualMembers(
          anyString,
          any[EventHandler],
          any[SlackBotProfile],
          any[DefaultServices]
        )(any[ActorSystem], any[ExecutionContext])
      }
    }

    "call sendForIndividualMembers when scheduled for individual members" in new TestContext {
      running(app) {
        val channel = "C12345678"
        val token = IDs.next
        val slackTeamId = "T1234567"
        val botProfile = SlackBotProfile("UMOCKBOT", team.id, None, slackTeamId, token, OffsetDateTime.now, allowShortcutMention = true)
        val sbSpy = scheduledBehaviorSpy(user, team, channel, isForIndividualMembers = true)
        val sent = sbSpy.send(eventHandler, botProfile, services, "Mock schedule")
        runNow(sent)
        verify(sbSpy, times(0)).sendFor(
          anyString,
          anyString,
          any[EventHandler],
          any[SlackBotProfile],
          any[DefaultServices]
        )(any[ActorSystem], any[ExecutionContext])
        verify(sbSpy, times(1)).sendForIndividualMembers(channel, eventHandler, botProfile, services)
      }
    }

    "not call sendFor or sendForIndividualMembers when in a DM and no user Slack profile exists" in new TestContext {
      running(app) {
        val channel = "D12345678"
        val token = IDs.next
        val slackTeamId = "T1234567"
        val botProfile = SlackBotProfile("UMOCKBOT", team.id, None, slackTeamId, token, OffsetDateTime.now, allowShortcutMention = true)
        when(services.dataService.users.maybeSlackProfileFor(user)).thenReturn(Future.successful(None))
        val sbSpy = scheduledBehaviorSpy(user, team, channel)
        val sent = sbSpy.send(eventHandler, botProfile, services, "Mock schedule")
        runNow(sent)
        verify(sbSpy, times(0)).sendFor(
          anyString,
          anyString,
          any[EventHandler],
          any[SlackBotProfile],
          any[DefaultServices]
        )(any[ActorSystem], any[ExecutionContext])
        verify(sbSpy, times(0)).sendForIndividualMembers(
          anyString,
          any[EventHandler],
          any[SlackBotProfile],
          any[DefaultServices]
        )(any[ActorSystem], any[ExecutionContext])
      }
    }
  }

  // TODO: These tests could be improved by testing the return value of updateOrDeleteScheduleAction
  // But it's unclear how to get the mock data service to reliably run DBIOActions and convert them into Futures
  "updateOrDeleteScheduleAction" should {
    "update if there is no times to run set on the recurrence" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        val sb = newScheduledBehavior(user, behavior, team, recurrence = Minutely(IDs.next, 1, 0, None))
        when(dataService.scheduledBehaviors.updateForNextRunAction(sb)).thenReturn(DBIO.successful(sb))
        sb.updateOrDeleteScheduleAction(dataService)
        verify(dataService.scheduledBehaviors, times(1)).updateForNextRunAction(sb)
        verify(dataService.scheduledBehaviors, times(0)).deleteAction(sb)
      }
    }

    "update if there is at least one more run after this on the recurrence" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        val sb = newScheduledBehavior(user, behavior, team, recurrence = Minutely(IDs.next, 1, 0, Some(2)))
        when(dataService.scheduledBehaviors.updateForNextRunAction(sb)).thenReturn(DBIO.successful(sb))
        sb.updateOrDeleteScheduleAction(dataService)
        verify(dataService.scheduledBehaviors, times(1)).updateForNextRunAction(sb)
        verify(dataService.scheduledBehaviors, times(0)).deleteAction(sb)
      }
    }

    "delete if there are no runs after this on the recurrence" in new TestContext {
      running(app) {
        val behavior = newBehavior(team)
        val sb = newScheduledBehavior(user, behavior, team, recurrence = Minutely(IDs.next, 1, 1, Some(2)))
        when(dataService.scheduledBehaviors.deleteAction(sb)).thenReturn(DBIO.successful(Some(sb)))
        sb.updateOrDeleteScheduleAction(dataService)
        verify(dataService.scheduledBehaviors, times(0)).updateForNextRunAction(sb)
        verify(dataService.scheduledBehaviors, times(1)).deleteAction(sb)
      }
    }
  }
}
