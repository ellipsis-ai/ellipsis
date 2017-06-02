import java.time.OffsetDateTime

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.scheduling.recurrence.Recurrence
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.team.Team
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.DataService
import support.TestContext

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

  def mockBehavior: Behavior = {
    mock[Behavior]
  }

  def mockScheduledBehavior(behavior: Behavior, team: Team): ScheduledBehavior = {
    ScheduledBehavior(
      "0",
      behavior,
      Map(),
      None,
      team,
      None,
      isForIndividualMembers = false,
      mock[Recurrence],
      OffsetDateTime.now,
      OffsetDateTime.now
    )
  }

  "displayText" should {
    "include the behavior name and behavior group name when both exist" in new TestContext {
      running(app) {
        val behavior = mockBehavior
        mockNames(dataService, behavior, Some("foo"), Some("bar"))
        val sb = mockScheduledBehavior(behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an action named `foo` in skill `bar`"""
      }
    }

    "include just the behavior name if no group name" in new TestContext {
      running(app) {
        val behavior = mockBehavior
        mockNames(dataService, behavior, Some("foo"), None)
        val sb = mockScheduledBehavior(behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an action named `foo`"""
      }
    }

    "say an unnamed action if there’s no action name" in new TestContext {
      running(app) {
        val behavior = mockBehavior
        mockNames(dataService, behavior, None, None)
        val sb = mockScheduledBehavior(behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an unnamed action"""
      }
    }

    "say an unnamed action with the skill name" in new TestContext {
      running(app) {
        val behavior = mockBehavior
        mockNames(dataService, behavior, None, Some("bar"))
        val sb = mockScheduledBehavior(behavior, team)
        val text = sb.displayText(dataService)
        runNow(text) mustBe """an unnamed action in skill `bar`"""
      }
    }
  }
}
