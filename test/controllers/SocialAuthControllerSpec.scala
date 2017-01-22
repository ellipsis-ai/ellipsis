package controllers

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.test._
import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.user.User
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.{ControllerTestContext, ControllerTestContextWithLoggedInUser}

import scala.concurrent.Future

class SocialAuthControllerSpec extends PlaySpec with MockitoSugar {

  def newLoginTokenFor(user: User, isExpired: Boolean = false): LoginToken = {
    val createdAt = if (isExpired) {
      OffsetDateTime.now.minusSeconds(LoginToken.EXPIRY_SECONDS + 1)
    } else {
      OffsetDateTime.now
    }
    LoginToken(IDs.next, user.id, createdAt)
  }

  "SocialAuthController.loginWithToken" should {

    "404 for nonexistent token" in new ControllerTestContext {
      running(app) {
        val nonExistentToken = IDs.next
        when(dataService.loginTokens.find(nonExistentToken)).thenReturn(Future.successful(None))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(nonExistentToken))).get
        status(result) mustBe NOT_FOUND
        assertNotJustLoggedIn(app, result)
        verify(dataService.loginTokens, times(1)).find(nonExistentToken)
      }
    }

    "Log in and redirect correctly for a valid token" in new ControllerTestContext {
      running(app) {
        val validToken = newLoginTokenFor(user)
        when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
        when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(user)))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect)))).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(redirect)
        assertUserJustLoggedIn(app, user, result)
        verify(dataService.loginTokens, times(1)).find(validToken.value)
      }
    }

    "Don't log in and inform the user if invalid token" in new ControllerTestContext {
      running(app) {
        val invalidToken = newLoginTokenFor(user, isExpired = true)
        when(dataService.loginTokens.find(invalidToken.value)).thenReturn(Future.successful(Some(invalidToken)))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(invalidToken.value, Some(redirect)))).get
        status(result) mustBe OK
        contentAsString(result) must include("expired")
        assertNotJustLoggedIn(app, result)
        verify(dataService.loginTokens, times(1)).find(invalidToken.value)
      }
    }

    "Change logged-in user if necessary" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val initiallyLoggedOutUser = newUserFor(teamId)
        val validToken = newLoginTokenFor(initiallyLoggedOutUser)
        when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
        when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(initiallyLoggedOutUser)))
        val request = FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect))).withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(redirect)
        assertUserJustLoggedIn(app, initiallyLoggedOutUser, result)
        verify(dataService.loginTokens, times(1)).find(validToken.value)
      }
    }

    "Redirect correctly, ignore the token, don't log in if already logged in as correct user" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val alreadyUsedToken = newLoginTokenFor(user, isExpired = true)
        when(dataService.loginTokens.find(alreadyUsedToken.value)).thenReturn(Future.successful(Some(alreadyUsedToken)))
        val request = FakeRequest(controllers.routes.SocialAuthController.loginWithToken(alreadyUsedToken.value, Some(redirect))).withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(redirect)
        assertNotJustLoggedIn(app, result)
        verify(dataService.loginTokens, times(1)).find(alreadyUsedToken.value)
      }
    }
  }

}
