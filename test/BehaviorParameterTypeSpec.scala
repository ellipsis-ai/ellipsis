import models.behaviors.behaviorparameter._
import models.behaviors.events.{SlackFile, SlackMessageEvent}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import support.TestContext

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class BehaviorParameterTypeSpec extends PlaySpec with MockitoSugar {
  "TextType" should {
    "coerce all JS values except null to JsString" in {
      TextType.prepareJsValue(JsString("foo")) mustEqual JsString("foo")
      TextType.prepareJsValue(JsBoolean(true)) mustEqual JsString("true")
      TextType.prepareJsValue(JsBoolean(false)) mustEqual JsString("false")
      TextType.prepareJsValue(JsArray(Seq(JsString("a"), JsString("b")))) mustEqual JsString("""["a","b"]""")
    }

    "let null be JsNull" in {
      TextType.prepareJsValue(JsNull) mustEqual JsNull
    }
  }

  "NumberType" should {
    "coerce valid numbers to JsNumber" in {
      NumberType.prepareJsValue(JsString("1")) mustEqual JsNumber(1)
      NumberType.prepareJsValue(JsString("1E10")) mustEqual JsNumber(1E10)
      NumberType.prepareJsValue(JsNumber(5.01)) mustEqual JsNumber(5.01)
    }

    "fallback to original for non-JSNumber-compatible values" in {
      NumberType.prepareJsValue(JsString("hey")) mustEqual JsString("hey")
      NumberType.prepareJsValue(JsString("0xFF")) mustEqual JsString("0xFF")
      NumberType.prepareJsValue(JsArray(Seq())) mustEqual JsArray(Seq())
      NumberType.prepareJsValue(JsNull) mustEqual JsNull
    }
  }

  "YesNoType" should {
    "coerce acceptable strings and booleans to JsBoolean" in {
      YesNoType.prepareJsValue(JsBoolean(true)) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsBoolean(false)) mustEqual JsBoolean(false)
      YesNoType.prepareJsValue(JsString("true")) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsString("yes")) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsString("false")) mustEqual JsBoolean(false)
      YesNoType.prepareJsValue(JsString("no")) mustEqual JsBoolean(false)
    }

    "fallback to original for non-boolean values" in {
      YesNoType.prepareJsValue(JsString("wtfbbq")) mustEqual JsString("wtfbbq")
      YesNoType.prepareJsValue(JsArray(Seq())) mustEqual JsArray(Seq())
      YesNoType.prepareJsValue(JsNull) mustEqual JsNull
    }
  }

  "FileType" should {

    def runNow[T](f: Future[T]): T = Await.result(f, 10.seconds)

    "coerce 'none' into JsNull" in {
      FileType.prepareJsValue(JsString("none")) mustEqual JsNull
    }

    "fallback to original for non-none values" in {
      FileType.prepareJsValue(JsString("wtfbbq")) mustEqual JsString("wtfbbq")
      FileType.prepareJsValue(JsArray(Seq())) mustEqual JsArray(Seq())
      FileType.prepareJsValue(JsNull) mustEqual JsNull
    }

    "if no file included, be valid if text is 'none', otherwise not valid" in new TestContext {
      val event = mock[SlackMessageEvent]
      when(event.maybeFile).thenReturn(None)
      val context = mock[BehaviorParameterContext]
      when(context.event).thenReturn(event)
      runNow(FileType.isValid("none", context)) mustBe true
      runNow(FileType.isValid("wtfbbq", context)) mustBe false
    }

    "if file included, be valid regardless of the text" in new TestContext {
      val event = mock[SlackMessageEvent]
      when(event.maybeFile).thenReturn(Some(SlackFile("https://fake-url.fake")))
      val context = mock[BehaviorParameterContext]
      when(context.event).thenReturn(event)
      runNow(FileType.isValid("none", context)) mustBe true
      runNow(FileType.isValid("wtfbbq", context)) mustBe true
    }
  }
}
