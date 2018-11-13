import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneId}

import models.IDs
import models.behaviors.behaviorparameter._
import models.behaviors.events.SlackFile
import models.behaviors.events.slack.SlackMessageEvent
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json._
import support.{DBSpec, TestContext}

class BehaviorParameterTypeSpec extends DBSpec {

  val team = Team(IDs.next, "Test team", Some(ZoneId.systemDefault), None, OffsetDateTime.now)

  def format(dateTime: OffsetDateTime): String = dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  "TextType" should {
    "coerce all JS values except null to JsString" in {
      TextType.prepareJsValue(JsString("foo"), team) mustEqual JsString("foo")
      TextType.prepareJsValue(JsBoolean(true), team) mustEqual JsString("true")
      TextType.prepareJsValue(JsBoolean(false), team) mustEqual JsString("false")
      TextType.prepareJsValue(JsArray(Seq(JsString("a"), JsString("b"))), team) mustEqual JsString("""["a","b"]""")
    }

    "let null be JsNull" in {
      TextType.prepareJsValue(JsNull, team) mustEqual JsNull
    }
  }

  "NumberType" should {
    "coerce valid numbers to JsNumber" in {
      NumberType.prepareJsValue(JsString("1"), team) mustEqual JsNumber(1)
      NumberType.prepareJsValue(JsString("1E10"), team) mustEqual JsNumber(1E10)
      NumberType.prepareJsValue(JsNumber(5.01), team) mustEqual JsNumber(5.01)
    }

    "fallback to original for non-JSNumber-compatible values" in {
      NumberType.prepareJsValue(JsString("hey"), team) mustEqual JsString("hey")
      NumberType.prepareJsValue(JsString("0xFF"), team) mustEqual JsString("0xFF")
      NumberType.prepareJsValue(JsArray(Seq()), team) mustEqual JsArray(Seq())
      NumberType.prepareJsValue(JsNull, team) mustEqual JsNull
    }
  }

  "DateTimeType" should {
    "coerce valid dates to JsStrings in ISO format" in {
      val twoPM = OffsetDateTime.now.withHour(14).withMinute(0).withSecond(0).withNano(0)
      DateTimeType.prepareJsValue(JsString("2pm"), team) mustEqual JsString(format(twoPM))
      DateTimeType.prepareJsValue(JsString("2pm yesterday"), team) mustEqual JsString(format(twoPM.minusDays(1)))
    }

    "fallback to original for non-compatible values" in {
      DateTimeType.prepareJsValue(JsString("hey"), team) mustEqual JsString("hey")
      DateTimeType.prepareJsValue(JsString("0xFF"), team) mustEqual JsString("0xFF")
      DateTimeType.prepareJsValue(JsArray(Seq()), team) mustEqual JsArray(Seq())
      DateTimeType.prepareJsValue(JsNull, team) mustEqual JsNull
    }
  }


  "YesNoType" should {
    "coerce acceptable strings and booleans to JsBoolean" in {
      YesNoType.prepareJsValue(JsBoolean(true), team) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsBoolean(false), team) mustEqual JsBoolean(false)
      YesNoType.prepareJsValue(JsString("true"), team) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsString("yes"), team) mustEqual JsBoolean(true)
      YesNoType.prepareJsValue(JsString("false"), team) mustEqual JsBoolean(false)
      YesNoType.prepareJsValue(JsString("no"), team) mustEqual JsBoolean(false)
    }

    "fallback to original for non-boolean values" in {
      YesNoType.prepareJsValue(JsString("wtfbbq"), team) mustEqual JsString("wtfbbq")
      YesNoType.prepareJsValue(JsArray(Seq()), team) mustEqual JsArray(Seq())
      YesNoType.prepareJsValue(JsNull, team) mustEqual JsNull
    }
  }

  "FileType" should {

    "coerce 'none' into JsNull" in {
      FileType.prepareJsValue(JsString("none"), team) mustEqual JsNull
    }

    "build an object with ID for non-none strings" in {
      FileType.prepareJsValue(JsString("wtfbbq"), team) mustEqual Json.toJson(Map("id" -> JsString("wtfbbq")))
    }

    "fallback to original for non-strings" in {
      FileType.prepareJsValue(JsArray(Seq()), team) mustEqual JsArray(Seq())
      FileType.prepareJsValue(JsNull, team) mustEqual JsNull
    }

    "if no file included, be valid if text is 'none', otherwise not valid" in new TestContext {
      val event = mock[SlackMessageEvent]
      when(event.maybeFile).thenReturn(None)
      val context = mock[BehaviorParameterContext]
      when(context.event).thenReturn(event)
      when(context.services).thenReturn(services)
      when(slackFileMap.maybeUrlFor(anyString)).thenReturn(None)
      runNow(FileType.isValidAction("none", context)) mustBe true
      runNow(FileType.isValidAction("wtfbbq", context)) mustBe false
    }

    "if file included, be valid regardless of the text" in new TestContext {
      val event = mock[SlackMessageEvent]
      when(event.maybeFile).thenReturn(Some(SlackFile("https://fake-url.fake", None)))
      val context = mock[BehaviorParameterContext]
      when(context.event).thenReturn(event)
      when(context.services).thenReturn(services)
      when(slackFileMap.maybeUrlFor(anyString)).thenReturn(None)
      runNow(FileType.isValidAction("none", context)) mustBe true
      runNow(FileType.isValidAction("wtfbbq", context)) mustBe true
    }

    "if the text matches an existing file ID, it's valid" in new TestContext {
      val event = mock[SlackMessageEvent]
      when(event.maybeFile).thenReturn(None)
      val context = mock[BehaviorParameterContext]
      when(context.event).thenReturn(event)
      when(context.services).thenReturn(services)
      val fileId = IDs.next
      when(slackFileMap.maybeUrlFor(fileId)).thenReturn(Some("https://fake-url.fake"))
      runNow(FileType.isValidAction(fileId, context)) mustBe true
    }
  }
}
