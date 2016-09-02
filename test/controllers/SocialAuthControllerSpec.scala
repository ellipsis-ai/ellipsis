package controllers

import com.mohiva.play.silhouette.api.crypto.{CookieSigner, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorSettings}
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
import play.api.inject.{BindingKey, bind}
import play.api.mvc.{RequestHeader, Result}
import services.DataService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class SocialAuthControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  implicit override lazy val app =
    new GuiceApplicationBuilder().
      overrides(bind[DataService].to[MockDataService]).
      build

  lazy val controller = app.injector.instanceOf(classOf[SocialAuthController])
  lazy val dataService = app.injector.instanceOf(classOf[DataService])
  lazy val cookieSigner = app.injector.instanceOf(BindingKey(classOf[CookieSigner]).qualifiedWith("authenticator-cookie-signer"))
  lazy val crypter = app.injector.instanceOf(BindingKey(classOf[Crypter]).qualifiedWith("authenticator-crypter"))
  lazy val encoder = new CrypterAuthenticatorEncoder(crypter)
  lazy val cookieAuthenticatorService = app.injector.instanceOf(classOf[AuthenticatorService[CookieAuthenticator]])
  lazy val cookieAuthenticatorSettings = app.configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
  lazy val authenticatorCookieName = app.configuration.getString("silhouette.authenticator.cookieName").get

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)
  def newLoginTokenFor(user: User, isUsed: Boolean = false): LoginToken = LoginToken(IDs.next, user.id, isUsed, DateTime.now)

  def newAuthenticatorFor(user: User)(implicit request: RequestHeader): CookieAuthenticator = {
    Await.result(cookieAuthenticatorService.create(user.loginInfo), 10.seconds)
  }

  def assertUserLoggedIn(user: User, result: Future[Result]): Unit = {
    val maybeAuthenticatorCookie = cookies(result).get(authenticatorCookieName)
    maybeAuthenticatorCookie mustNot be(None)
    CookieAuthenticator.unserialize(maybeAuthenticatorCookie.get.value, cookieSigner, encoder) match {
      case Success(authenticator: CookieAuthenticator) => authenticator.loginInfo.providerKey mustBe user.id
      case Failure(e) => throw e
    }
  }

  def assertNotLoggedIn(result: Future[Result]): Unit = {
    cookies(result).get(authenticatorCookieName) mustBe None
  }

  "SocialAuthController.loginWithToken" should {

    "404 for nonexistent token" in {
      val nonExistentToken = IDs.next
      when(dataService.loginTokens.find(nonExistentToken)).thenReturn(Future.successful(None))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(nonExistentToken))).get
      status(result) mustBe NOT_FOUND
      assertNotLoggedIn(result)
      verify(dataService.loginTokens, times(1)).find(nonExistentToken)
    }

    "Redirect correctly for a valid token" in {
      val redirect = "/whatever"
      val teamId = IDs.next
      val user = newUserFor(teamId)
      val validToken = newLoginTokenFor(user)
      when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
      when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(user)))
      when(dataService.loginTokens.use(validToken)).thenReturn(Future.successful({}))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect)))).get
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(redirect)
      cookies(result).get(authenticatorCookieName) mustNot be(None)
      assertUserLoggedIn(user, result)
      verify(dataService.loginTokens, times(1)).find(validToken.value)
    }

    "Inform the user if invalid token" in {
      val redirect = "/whatever"
      val teamId = IDs.next
      val user = newUserFor(teamId)
      val invalidToken = newLoginTokenFor(user, isUsed=true)
      when(dataService.loginTokens.find(invalidToken.value)).thenReturn(Future.successful(Some(invalidToken)))
      val result = route(FakeRequest(controllers.routes.SocialAuthController.loginWithToken(invalidToken.value, Some(redirect)))).get
      status(result) mustBe OK
      contentAsString(result) must include("expired")
      assertNotLoggedIn(result)
      verify(dataService.loginTokens, times(1)).find(invalidToken.value)
    }

    "Change logged-in user if necessary" in {
      val redirect = "/whatever"
      val teamId = IDs.next
      val wrongUser = newUserFor(teamId)
      val rightUser = newUserFor(teamId)
      val validToken = newLoginTokenFor(rightUser)
      when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
      when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(rightUser)))
      when(dataService.loginTokens.use(validToken)).thenReturn(Future.successful({}))
      val request = FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect)))
      //val userAwareRequest = controller.UserAwareRequest(Some(wrongUser), Some(newAuthenticatorFor(wrongUser)(request)), request)
      val result = route(request).get
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(redirect)
      assertUserLoggedIn(rightUser, result)
      verify(dataService.loginTokens, times(1)).find(validToken.value)
    }

// TODO: make this test work after upgrading Silhouette
//    "Redirect correctly and ignore the token if already logged in as correct user" in {
//      val redirect = "/whatever"
//      val teamId = IDs.next
//      val user = newUserFor(teamId)
//      val otherUser = newUserFor(teamId)
//      val invalidToken = newLoginTokenFor(otherUser, isUsed = true)
//      when(dataService.loginTokens.find(invalidToken.value)).thenReturn(Future.successful(Some(invalidToken)))
//      val request = FakeRequest(controllers.routes.SocialAuthController.loginWithToken(invalidToken.value, Some(redirect)))
//      //val userAwareRequest = controller.UserAwareRequest(Some(user), Some(newAuthenticatorFor(user)(request)), request)
//      val result = route(request).get
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result) mustBe Some(redirect)
//      assertUserLoggedIn(user, result)
//      verify(dataService.loginTokens, times(0)).find(invalidToken.value)
//    }

  }

}
