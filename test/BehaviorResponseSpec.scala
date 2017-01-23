import models.IDs
import models.behaviors.BehaviorResponse
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.behaviors.input.Input
import models.behaviors.triggers.TemplateMessageTrigger
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import support.TestContext

import scala.concurrent.Future

class BehaviorResponseSpec extends PlaySpec with MockitoSugar{

  "BehaviorResponse" should {

    "choose the most specific trigger" in new TestContext {
      running(app) {
        val event = mock[MessageEvent]
        when(event.relevantMessageText).thenReturn("trigger me this batman")
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
        val fooParam = BehaviorParameter(IDs.next, 1, Input(IDs.next, "foo", None, TextType, false, false, None), version)
        val barParam = fooParam.copy(id = IDs.next, input = Input(IDs.next, "bar", None, TextType, false, false, None), rank = 2)
        when(dataService.behaviorParameters.allFor(version)).
          thenReturn(Future.successful(Seq(fooParam, barParam)))
        val responses = await(BehaviorResponse.allFor(event, Some(team), None, lambdaService, dataService, cache, ws, configuration))
        responses must have length(1)
        responses.head.activatedTrigger must equal(specificTrigger)
      }
    }

  }

}
