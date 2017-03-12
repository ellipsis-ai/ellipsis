package controllers.api.dev.v1

import models.IDs
import models.team.Team
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContext

import scala.concurrent.Future

/**
  * Created by matteo on 11/29/16.
  */
  class SmallStorageControllerSpec extends PlaySpec with MockitoSugar {

  "SmallStorageController.show" should {

    "returns 404 when token is not passed" in new ControllerTestContext {
      running(app) {
        val request = FakeRequest(controllers.api.dev.v1.routes.SmallStorageController.show(""))
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
      }
    }

    "returns 200 when a team token is passed " in new ControllerTestContext {
      running(app) {
        val token = "pippo"
        val team = Team(IDs.next, "matteo")
        when(dataService.teams.findForToken(token)).thenReturn(Future.successful(Some(team)))
        val request = FakeRequest(controllers.api.dev.v1.routes.SmallStorageController.show(token))
        val result = route(app, request).get

        status(result) mustBe OK
        contentType(result) mustBe Some("application/json")

        val json = Json.parse(contentAsString(result))

        (json \ "status").as[String] mustBe("OK")
        (json \ "team").as[String] mustEqual(team.name)
      }
    }
  }

}
