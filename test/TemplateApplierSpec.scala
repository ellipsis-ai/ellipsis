import models.bots.templates.TemplateApplier
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

    "apply for iteration over a list" in {
      val resultJson = Json.toJson(Map("items" -> Array("first", "second", "third")))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. first\n\n1. second\n\n1. third"
    }

    "apply for iteration over a list of non-strings" in {
      val resultJson = Json.toJson(Map("items" -> Array(1, 2, 3)))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. 1\n\n1. 2\n\n1. 3"
    }

    "apply for iteration over a list of objects" in {
      val resultJson = Json.toJson(Map("items" -> Array( Map("value" -> 1), Map("value" -> 2), Map("value" -> 3))))
      val applier = TemplateApplier(Some(
        """{for item in successResult.items}
          |1. {item.value}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. 1\n\n1. 2\n\n1. 3"
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
      applier.apply.trim mustBe "1. first\n\n1. second\n\n1. third\n\n\n\n- first\n\n- second\n\n- third"
    }

    "apply for iteration over top-level list result" in {
      val resultJson = Json.toJson(Array("first", "second", "third"))
      val applier = TemplateApplier(Some(
        """{for item in successResult}
          |1. {item}
          |{endfor}
        """.stripMargin), JsDefined(resultJson))
      applier.apply.trim mustBe "1. first\n\n1. second\n\n1. third"
    }

    "apply for nested iterations" in {
      val result = Json.toJson(Map( "numbers" -> Array("first", "second", "third"), "letters" -> Array("a", "b", "c")))
      val applier = TemplateApplier(Some(
        """{for number in successResult.numbers}
          |{for letter in successResult.letters}
          |1. {number} and {letter}
          |{endfor}
          |{endfor}
        """.stripMargin), JsDefined(result))
      val expected = "1. first and a\n\n1. first and b\n\n1. first and c\n\n" ++
        "\n\n1. second and a\n\n1. second and b\n\n1. second and c\n\n" ++
        "\n\n1. third and a\n\n1. third and b\n\n1. third and c"

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

  }

}
