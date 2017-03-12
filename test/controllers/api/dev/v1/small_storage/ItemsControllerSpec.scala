package controllers.api.dev.v1.small_storage

import models.IDs
import models.team.Team
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContext

import scala.concurrent.Future

/**
  * Created by matteo on 11/29/16.
  */
class ItemsControllerSpec extends PlaySpec with MockitoSugar {

  "ItemsController#index" should {
    "returns 404 when token is invalid" in new ControllerTestContext {
      running(app) {
        val request = FakeRequest(controllers.api.dev.v1.small_storage.routes.ItemsController.index(""))
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
      }
    }
    "returns 200, when token is valid" in new ControllerTestContext {
      running(app) {
        val token = "pippo"
        val team = Team(IDs.next, "matteo")
        when(dataService.teams.findForToken(token)).thenReturn(Future.successful(Some(team)))
        val request = FakeRequest(controllers.api.dev.v1.small_storage.routes.ItemsController.index(token))
        val result = route(app, request).get

        status(result) mustBe OK
        contentType(result) mustBe Some("application/json")
      }
    }
    "returns a list of empty items, with a valid token" in new ControllerTestContext {
      running(app) {
        val token = "pippo"
        val team = Team(IDs.next, "matteo")
        when(dataService.teams.findForToken(token)).thenReturn(Future.successful(Some(team)))

        val request = FakeRequest(controllers.api.dev.v1.small_storage.routes.ItemsController.index(token))
        val result = route(app, request).get

        status(result) mustBe OK

        val json = Json.parse(contentAsString(result))
        (json \ "object").as[String]  mustBe("list")
        (json \ "has_more").as[Boolean] mustBe(false)
        (json \ "total_count").as[Int] mustBe(0)
        (json \ "url").as[String] mustBe("api/dev/v1/small_storage/items")
        (json \ "data").as[JsArray].value.length mustBe(0)
      }
    }
    "returns a list of items, with a valid token" in new ControllerTestContext {
      running(app) {
        val token = "pippo"
        val team = Team(IDs.next, "matteo")
        when(dataService.teams.findForToken(token)).thenReturn(Future.successful(Some(team)))

        val request = FakeRequest(controllers.api.dev.v1.small_storage.routes.ItemsController.index(token))
        val result = route(app, request).get

        status(result) mustBe OK

        val json = Json.parse(contentAsString(result))
        (json \ "object").as[String]  mustBe("list")
        (json \ "has_more").as[Boolean] mustBe(false)
        (json \ "total_count").as[Int] mustBe(0)
        (json \ "url").as[String] mustBe("api/dev/v1/small_storage/items")
        (json \ "data").as[JsArray].value.length mustBe(0)
      }
    }
  }
}
