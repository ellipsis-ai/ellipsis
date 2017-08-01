import models.IDs
import models.behaviors.{BehaviorResponse, ParameterValue, ParameterWithValue}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.testing.TestEvent
import models.behaviors.triggers.TemplateMessageTrigger
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
import play.api.test.Helpers._
import slick.dbio.DBIO
import support.TestContext

import scala.concurrent.Future

class BehaviorResponseSpec extends PlaySpec with MockitoSugar {

  "BehaviorResponse" should {

    "choose the most specific trigger" in new TestContext {
      running(app) {
        val event = TestEvent(user, team, "trigger me this batman", includesBotMention = true)
        val version = mock[BehaviorVersion]
        val generalTrigger = TemplateMessageTrigger(
          IDs.next,
          version,
          "trigger me",
          requiresBotMention = false,
          isCaseSensitive= false
        )
        val mediumTrigger = generalTrigger.copy(id = IDs.next, template = "trigger me {foo}")
        val specificTrigger = generalTrigger.copy(id = IDs.next, template = "trigger me {foo} {bar}")
        when(dataService.messageTriggers.allActiveFor(team)).
          thenReturn(Future.successful(Seq(generalTrigger, mediumTrigger, specificTrigger)))
        val groupVersion = mock[BehaviorGroupVersion]
        val fooParam = BehaviorParameter(IDs.next, 1, Input(IDs.next, IDs.next, None, "foo", None, TextType, false, false, groupVersion), version)
        val barParam = fooParam.copy(id = IDs.next, input = Input(IDs.next, IDs.next, None, "bar", None, TextType, false, false, groupVersion), rank = 2)
        val params = Seq(fooParam, barParam)
        when(dataService.behaviorParameters.allFor(version)).thenReturn(Future.successful(params))
        when(dataService.behaviorParameters.allForAction(version)).thenReturn(DBIO.successful(params))
        val paramsWithValues = Seq(
          ParameterWithValue(fooParam, "param0", Some(ParameterValue("param0", JsString("this"), isValid = true))),
          ParameterWithValue(barParam, "param1", Some(ParameterValue("param1", JsString("batman"), isValid = true)))
          )
        when(dataService.behaviorResponses.buildFor(event, version, specificTrigger.invocationParamsFor(event, params), Some(specificTrigger), None)).thenReturn(
          Future.successful(BehaviorResponse(event, version, None, paramsWithValues, Some(specificTrigger), services))
        )
        val responses = await(event.allBehaviorResponsesFor(Some(team), None, services))
        responses must have length(1)
        responses.head.maybeActivatedTrigger must contain(specificTrigger)
      }
    }

  }

}
