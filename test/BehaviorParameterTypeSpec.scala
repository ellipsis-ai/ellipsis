import models.behaviors.behaviorparameter._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class BehaviorParameterTypeSpec extends PlaySpec {
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
}
