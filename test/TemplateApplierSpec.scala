import _root_.util.TemplateApplier
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
      applier.apply mustBe "Not found"
    }

  }

}
