package controllers

import java.time.OffsetDateTime

import json.Formatting._
import json.{APIErrorData, APIResultWithErrorsData, LogEntryData}
import models.IDs
import models.accounts.user.User
import models.behaviors.ResultType
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.events.EventType
import models.behaviors.invocationlogentry.InvocationLogEntry
import models.behaviors.invocationtoken.InvocationToken
import models.team.Team
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataService
import support.ControllerTestContext

import scala.concurrent.Future

class InvocationLogControllerSpec extends PlaySpec with MockitoSugar {

  val token: String = IDs.next
  val defaultSlackTeamId: String = IDs.next
  val behaviorName = "test"
  val now: OffsetDateTime = OffsetDateTime.now

  def makeLogs(behaviorVersion: BehaviorVersion, user: User): Seq[InvocationLogEntry] = {
    val entry = InvocationLogEntry(
      IDs.next,
      behaviorVersion,
      ResultType.Success.toString,
      Some(EventType.chat),
      Some(EventType.chat),
      "test",
      JsObject(Seq()),
      "done",
      "test",
      Some("channel"),
      None,
      user,
      1000,
      None,
      now.minusHours(1)
    )
    Seq(
      entry.copy(createdAt = entry.createdAt.minusHours(3), maybeOriginalEventType = Some(EventType.test)),
      entry.copy(createdAt = entry.createdAt.minusHours(2), maybeOriginalEventType = Some(EventType.scheduled)),
      entry.copy(createdAt = entry.createdAt.minusHours(1)),
      entry
    )
  }

  def setupLogs(user: User, team: Team, dataService: DataService): Seq[InvocationLogEntry] = {
    val group = BehaviorGroup(IDs.next, None, team, now)
    val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
    val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
    val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
    val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, Some(behaviorName), None, None, Normal, false, false, OffsetDateTime.now)
    val invocationToken = InvocationToken(IDs.next, user.id, originatingBehaviorVersion.id, None, Some(defaultSlackTeamId), now)

    val logs = makeLogs(targetBehaviorVersion, user)
    when(dataService.invocationTokens.findNotExpired(token)).thenReturn(Future.successful(Some(invocationToken)))
    when(dataService.invocationTokens.findNotExpired("wrong")).thenReturn(Future.successful(None))
    when(dataService.behaviorVersions.findWithoutAccessCheck(invocationToken.behaviorVersionId)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
    when(dataService.behaviorVersions.findByName(behaviorName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
    when(dataService.behaviorVersions.findByName("wrong", groupVersion)).thenReturn(Future.successful(None))
    when(dataService.invocationLogEntries.allForBehavior(same(targetBehaviorVersion.behavior), any[OffsetDateTime], any[OffsetDateTime], same(None), same(None))).thenReturn(Future.successful(logs))
    when(dataService.invocationLogEntries.allForBehavior(same(targetBehaviorVersion.behavior), any[OffsetDateTime], any[OffsetDateTime], same(None), Matchers.eq(Some(EventType.chat)))).thenReturn {
      Future.successful(logs.filter(_.maybeOriginalEventType.exists(_ == EventType.chat)))
    }
    logs
  }

  def maybeErrorInResult(jsResult: JsValue): Option[APIErrorData] = {
    jsResult.validate[APIResultWithErrorsData] match {
      case JsSuccess(data, _) => data.errors.headOption
      case JsError(_) => None
    }
  }

  "getLogs" should {
    "return a 400 with an invalid token" in new ControllerTestContext {
      running(app) {
        setupLogs(user, team, dataService)
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          behaviorName, "wrong", None, None, None, None
        ))
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorInResult(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid or expired token", Some("token")))
      }
    }

    "return a 404 with a non-existent behavior" in new ControllerTestContext {
      running(app) {
        setupLogs(user, team, dataService)
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          "wrong", token, None, None, None, None
        ))
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
        maybeErrorInResult(contentAsJson(result)) mustEqual Some(APIErrorData(InvocationLogController.noActionFoundMessage("wrong"), Some("actionName")))
      }
    }

    "return a list of log entries with a valid behavior and no event type" in new ControllerTestContext {
      running(app) {
        val logs = setupLogs(user, team, dataService)
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          behaviorName, token, None, None, None, None
        ))
        val result = route(app, request).get
        status(result) mustBe OK
        verify(dataService.invocationLogEntries, times(1)).allForBehavior(any[Behavior], any[OffsetDateTime], any[OffsetDateTime], same(None), same(None))
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[LogEntryData]] match {
          case JsSuccess(data, jsPath) => {
            data.length mustEqual logs.length
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }

    "return a list of log entries with a valid behavior and valid event type" in new ControllerTestContext {
      running(app) {
        val logs = setupLogs(user, team, dataService)
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          behaviorName, token, None, None, None, Some(EventType.chat.toString)
        ))
        val result = route(app, request).get
        status(result) mustBe OK
        verify(dataService.invocationLogEntries, times(1)).allForBehavior(any[Behavior], any[OffsetDateTime], any[OffsetDateTime], same(None), Matchers.eq(Some(EventType.chat)))
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[LogEntryData]] match {
          case JsSuccess(data, jsPath) => {
            data.length mustEqual logs.count(_.maybeOriginalEventType.exists(_ == EventType.chat))
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }

    "return an empty list with a valid behavior but invalid original event type" in new ControllerTestContext {
      running(app) {
        setupLogs(user, team, dataService)
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          behaviorName, token, None, None, None, Some("nonexistentEventType")
        ))
        val result = route(app, request).get
        status(result) mustBe OK
        verify(dataService.invocationLogEntries, never()).allForBehavior(any[Behavior], any[OffsetDateTime], any[OffsetDateTime], same(None), any[Option[EventType]])
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[LogEntryData]] match {
          case JsSuccess(data, jsPath) => {
            data mustBe empty
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }
  }
}
