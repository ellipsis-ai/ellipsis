package controllers

import mocks._
import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.user.User
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import services.DataService

import scala.concurrent.Future

class SocialAuthControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  implicit override lazy val app =
    new GuiceApplicationBuilder().
      overrides(bind[DataService].to[MockDataService]).
      build

  lazy val dataService = app.injector.instanceOf(classOf[DataService])

  "SocialAuthController.loginWithToken" should {

    "404 for nonexistent token" in {
      val nonExistentToken = "nonexistent_token"
      when(dataService.loginTokens.find(nonExistentToken)).thenReturn(Future.successful(None))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(nonExistentToken))).get
      status(result) mustBe NOT_FOUND
      verify(dataService.loginTokens, times(1)).find(nonExistentToken)
    }

    "Redirect correctly for a valid token" in {
      val redirect = "/whatever"
      val teamId = IDs.next
      val user = User(IDs.next, teamId, None)
      val validToken = LoginToken("valid_token", user.id, isUsed=false, DateTime.now)
      when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
      when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(user)))
      when(dataService.loginTokens.use(validToken)).thenReturn(Future.successful({}))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect)))).get
      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(redirect)
      verify(dataService.loginTokens, times(1)).find(validToken.value)
    }

    "Inform the user if invalid token" in {
      val redirect = "/whatever"
      val teamId = IDs.next
      val user = User(IDs.next, teamId, None)
      val invalidToken = LoginToken("invalid_token", user.id, isUsed=true, DateTime.now)
      when(dataService.loginTokens.find(invalidToken.value)).thenReturn(Future.successful(Some(invalidToken)))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(invalidToken.value, Some(redirect)))).get
      status(result) mustBe OK
      contentAsString(result) must include("expired")
      verify(dataService.loginTokens, times(1)).find(invalidToken.value)
    }

  }

}
