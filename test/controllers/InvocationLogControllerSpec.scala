package controllers

import java.time.OffsetDateTime

import json.LogEntryData
import models.IDs
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.invocationtoken.InvocationToken
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContext
import json.Formatting._

import scala.concurrent.Future

class InvocationLogControllerSpec extends PlaySpec with MockitoSugar {

  "getLogs" should {
    "return an empty list with a valid behavior but invalid original event type" in new ControllerTestContext {
      running(app) {
        val now = OffsetDateTime.now
        val token = IDs.next
        val group = BehaviorGroup(IDs.next, None, team, None, now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val behaviorName = "test"
        val invocationToken = InvocationToken(IDs.next, user.id, originatingBehavior.id, None, now)
        when(dataService.invocationTokens.findNotExpired(token)).thenReturn(Future.successful(Some(invocationToken)))
        when(dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrNameOrTrigger(behaviorName, group)).thenReturn(Future.successful(Some(targetBehavior)))
        val request = FakeRequest(controllers.routes.InvocationLogController.getLogs(
          behaviorName, token, None, None, None, Some("nonexistentEventType")
        ))
        val result = route(app, request).get
        status(result) mustBe OK
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
