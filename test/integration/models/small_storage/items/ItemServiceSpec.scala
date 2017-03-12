package integration.models.small_storage.items

import scala.concurrent.duration._
import scala.concurrent.Await
import play.api.libs.json._
import play.api.test.Helpers.running
import org.scalatestplus.play.PlaySpec
import support.TestContext
import models.small_storage.items._
import org.scalatest.mock.MockitoSugar


class ItemServiceSpec extends PlaySpec with MockitoSugar{

  "ItemService#create" should {
    "return the new Item" in new TestContext {
      running(app) {
        val s1: JsValue = Json.parse(
          """
            |{
            |  "name" : "staging1",
            |  "url" : "staging1.ellipsis.ai",
            |  "user" : "matteo"
            |}
            |""".stripMargin)

        val s1Stored: Item = Await.result(itemService.create(`team` = team, kind = "Staging", data = s1.as[JsObject]), 10.seconds)
        val s1Name: String = (s1Stored.data \ "name").as[String]
        s1Name mustBe "staging1"
        s1Stored.id mustBe a [String]
        Option(s1Stored.id).forall(_.isEmpty) mustBe false
      }
    }
  }
  "ItemService#save" should {
    "return the new Item" in new TestContext {
      running(app) {
        val s1: JsValue = Json.parse(
          """
            |{
            |  "name" : "staging1",
            |  "url" : "staging1.ellipsis.ai",
            |  "user" : "matteo"
            |}
            |""".stripMargin)

        val s1Stored: Item = Await.result(itemService.create(`team` = team, kind = "Staging", data = s1.as[JsObject]), 10.seconds)
        val s1Name: String = (s1Stored.data \ "name").as[String]
        s1Name mustBe "staging1"
        s1Stored.id mustBe a [String]
        Option(s1Stored.id).forall(_.isEmpty) mustBe false
      }
    }
  }
}
