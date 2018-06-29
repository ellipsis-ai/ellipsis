import models.behaviors.templates.TemplateApplier
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class TemplateApplierSpec extends PlaySpec {

  "TemplateApplier" should {

    "apply to a single-level object result" in  {
      val json = Json.toJson(Map("foo" -> "2"))
      val applier = TemplateApplier(Some("{successResult.foo}"), JsDefined(json))
      applier.apply mustBe "2"
    }

    "apply to a multi-level object result" in  {
      val json = Json.toJson(Map("foo" -> Map("bar" -> "2")))
      val applier = TemplateApplier(Some("{successResult.foo.bar}"), JsDefined(json))
      applier.apply mustBe "2"
    }

    "apply when template has multiple refs" in {
      val json = Json.toJson(Map("foo" -> "2", "bar" -> "3"))
      val applier = TemplateApplier(Some("foo: {successResult.foo} bar: {successResult.bar}"), JsDefined(json))
      applier.apply mustBe "foo: 2 bar: 3"
    }

    "apply to plain strings" in {
      val json = Json.toJson("3")
      val applier = TemplateApplier(Some("{successResult}"), JsDefined(json))
      applier.apply mustBe "3"
    }

    "apply to plain numbers" in {
      val json = Json.toJson(2)
      val applier = TemplateApplier(Some("{successResult}"), JsDefined(json))
      applier.apply mustBe "2"
    }

    "apply for non-string values" in {
      val json = Json.toJson(Map("foo" -> 2))
      val applier = TemplateApplier(Some("{successResult.foo}"), JsDefined(json))
      applier.apply mustBe "2"
    }

    "apply for missing property" in {
      val json = Json.toJson(Map("foo" -> 2))
      val applier = TemplateApplier(Some("{successResult.bar}"), JsDefined(json))
      applier.apply mustBe "**successResult.bar not found**"
    }

    "apply inputs too" in {
      val resultJson = Json.toJson(Map("total" -> "3"))
      val inputs = Seq(("userInput1", JsString("1")), ("userInput2", JsString("2")))
      val applier = TemplateApplier(Some("{userInput1} + {userInput2} = {successResult.total}"), JsDefined(resultJson), inputs)
      applier.apply mustBe "1 + 2 = 3"
    }

    "apply missing input" in {
      val resultJson = Json.toJson(Map("total" -> "3"))
      val inputs = Seq(("userInput1", JsString("1")), ("userInput2", JsString("2")))
      val applier = TemplateApplier(Some("{userInput1} + {madeUp} = {successResult.total}"), JsDefined(resultJson), inputs)
      applier.apply mustBe "1 + **madeUp not found** = 3"
    }

    "apply for iteration over a list without extra line breaks" in {
      val resultJson = Json.toJson(Map("items" -> Array("first", "second", "third")))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. first\n1. second\n1. third"
    }

    "apply for iteration over a list of non-strings" in {
      val resultJson = Json.toJson(Map("items" -> Array(1, 2, 3)))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. 1\n1. 2\n1. 3"
    }

    "apply for iteration over a list of objects" in {
      val resultJson = Json.toJson(Map("items" -> Array( Map("value" -> 1), Map("value" -> 2), Map("value" -> 3))))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item.value}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. 1\n1. 2\n1. 3"
    }

    "apply for multiple iterations over a list" in {
      val resultJson = Json.toJson(Map("items" -> Array("first", "second", "third")))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item}
          |{endfor}
          |
          |{for ea in successResult.items}
          |- {ea}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. first\n1. second\n1. third\n\n- first\n- second\n- third"
    }

    "apply for iteration over top-level list result" in {
      val resultJson = Json.toJson(Array("first", "second", "third"))
      val applier = TemplateApplier(Some(
        """{for item in successResult}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. first\n1. second\n1. third"
    }

    "apply for nested iterations without extra line breaks" in {
      val result = Json.toJson(Map( "numbers" -> Array("first", "second", "third"), "letters" -> Array("a", "b", "c")))
      val applier = TemplateApplier(Some(
        """{for number in successResult.numbers}
          |{for letter in successResult.letters}
          |1. {number} and {letter}
          |{endfor}
          |{endfor}
        """.stripMargin), JsDefined(result))
      val expected = "1. first and a\n1. first and b\n1. first and c\n" ++
        "1. second and a\n1. second and b\n1. second and c\n" ++
        "1. third and a\n1. third and b\n1. third and c"

      applier.apply.trim mustBe expected
    }

    "handle missing iteration list" in {
      val result = Json.toJson(Map( "numbers" -> Array("first", "second", "third") ))
      val applier = TemplateApplier(Some(
        """{for item in successResult.letters}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(result))
      applier.apply.trim mustBe "**successResult.letters not found**"
    }

    "handle missing nested iteration list" in {
      val result = Json.parse("""{ "lists": [ { "first": [] } ] }""")
      val applier = TemplateApplier(Some(
        """{for list in successResult.lists}
          |{for item in list.second}
          |1. {item}
          |{endfor}
          |{endfor}
        """.stripMargin), JsDefined(result))
      applier.apply.trim mustBe "**list.second not found**"
    }

    "apply for a true conditional" in {
      val resultJson = Json.toJson(Map("shouldDisplay" -> true))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "displayed"
    }

    "apply for a false conditional" in {
      val resultJson = Json.toJson(Map("shouldDisplay" -> false))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe ""
    }

    "handle non-boolean condition" in {
      val resultJson = Json.toJson(Map("shouldDisplay" -> "non-boolean"))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe """**successResult.shouldDisplay is not a boolean** ("non-boolean")"""
    }

    "apply for nested true conditionals" in {
      val resultJson = Json.toJson(Map("isHighEnough" -> true, "shouldDisplay" -> true))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{if successResult.isHighEnough}
          |and high enough
          |{endif}
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "displayed\nand high enough"
    }

    "apply for conditional in iteration without extra line breaks" in {
      val result = Json.parse(
        """{ "numbers": [
          |{ "number": 1, "isEven": false },
          |{ "number": 2, "isEven": true },
          |{ "number": 3, "isEven": false },
          |{ "number": 4, "isEven": true }
          ] }""".stripMargin)
      val applier = TemplateApplier(Some(
        """{for ea in successResult.numbers}
          |{if ea.isEven}
          |Even number alert!
          |{ea.number} is even
          |{endif}
          |{endfor}
        """.stripMargin), JsDefined(result))

      applier.apply.trim mustBe "Even number alert!\n2 is even\nEven number alert!\n4 is even"
    }

    "apply for a true conditional with an else clause" in {
      val resultJson = Json.toJson(Map("shouldDisplay" -> true))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{else}
          |not displayed
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "displayed"
    }

    "apply for a false conditional with an else clause" in {
      val resultJson = Json.toJson(Map("shouldDisplay" -> false))
      val applier = TemplateApplier(Some(
        """{if successResult.shouldDisplay}
          |displayed
          |{else}
          |not displayed
          |{endif}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "not displayed"
    }

    "preserve desired line breaks and paragraph breaks" in {
      val result = Json.toJson(Map( "numbers" -> Array("first", "second") ))
      val applier = TemplateApplier(Some(
        """{for n in successResult.numbers}
          |The next number is:
          |- {n}
          |
          |{endfor}
        """.stripMargin
      ), JsDefined(result))
      applier.apply.trim mustBe "The next number is:\n- first\n\nThe next number is:\n- second"
    }

    "handle the same name used for both input param and successResult property" in {
      val resultJson = Json.toJson(Map("sameName" -> "in result"))
      val inputs = Seq(("sameName", JsString("from input")))
      val applier = TemplateApplier(Some("In result: {successResult.sameName}, from input: {sameName}"), JsDefined(resultJson), inputs)
      applier.apply mustBe "In result: in result, from input: from input"
    }

    "handle the same name used for both input param and (shadowing) iteration param" in {
      val resultJson = Json.toJson(Map("items" -> Array("first", "second")))
      val inputs = Seq(("sameName", JsString("from input")))
      val applier = TemplateApplier(Some("""From input: {sameName}
                                           |{for sameName in successResult.items}
                                           |- {sameName}
                                           |{endfor}
                                         """.stripMargin), JsDefined(resultJson), inputs)
      applier.apply.trim mustBe "From input: from input\n- first\n- second"
    }

    "output a JsString as a string if there is no template" in {
      val resultJson = JsString("two\nlines\n")
      val applier = TemplateApplier(None, JsDefined(resultJson))
      applier.apply mustBe "two\nlines\n"
    }

    "output a non-string JsValue as a JSON-pretty-printed string" in {
      val resultJson = Json.obj("foo" -> "bar")
      val applier = TemplateApplier(None, JsDefined(resultJson))
      applier.apply mustBe
        """{
          |  "foo" : "bar"
          |}""".stripMargin
    }

  }

}
