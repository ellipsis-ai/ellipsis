import models.IDs
import models.behaviors.BehaviorResponse
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{MessageContext, MessageEvent}
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
        val event = new MessageEvent {
          val context: MessageContext = mock[MessageContext]
        }
        when(event.context.relevantMessageText).thenReturn("trigger me this batman")
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
        val fooParam = BehaviorParameter(IDs.next, "foo", 1, version, None, TextType)
        val barParam = fooParam.copy(id = IDs.next, name = "bar", rank = 2)
        when(dataService.behaviorParameters.allFor(version)).
          thenReturn(Future.successful(Seq(fooParam, barParam)))
        val responses = await(BehaviorResponse.allFor(event, Some(team), None, lambdaService, dataService, cache))
        responses must have length(1)
        responses.head.activatedTrigger must equal(specificTrigger)
      }
    }

  }

}
