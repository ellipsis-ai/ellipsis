package controllers

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import mocks._
import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import modules.{SilhouetteModule, TestSilhouetteModule}
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{BindingKey, bind}
import play.api.mvc.Result
import services.DataService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class SocialAuthControllerSpec extends PlaySpec with MockitoSugar {

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)
  def newLoginTokenFor(user: User, isUsed: Boolean = false): LoginToken = LoginToken(IDs.next, user.id, isUsed, DateTime.now)

  def assertUserJustLoggedIn(app: Application, user: User, result: Future[Result]): Unit = {
    val cookieSigner = app.injector.instanceOf(BindingKey(classOf[CookieSigner]).qualifiedWith("authenticator-cookie-signer"))
    val encoder = new Base64AuthenticatorEncoder
    val authenticatorCookieName = app.configuration.getString("silhouette.authenticator.cookieName").get
    val maybeAuthenticatorCookie = cookies(result).get(authenticatorCookieName)
    maybeAuthenticatorCookie mustNot be(None)
    CookieAuthenticator.unserialize(maybeAuthenticatorCookie.get.value, cookieSigner, encoder) match {
      case Success(authenticator: CookieAuthenticator) => authenticator.loginInfo.providerKey mustBe user.id
      case Failure(e) => throw e
    }
  }

  def assertNotJustLoggedIn(app: Application, result: Future[Result]): Unit = {
    val authenticatorCookieName = app.configuration.getString("silhouette.authenticator.cookieName").get
    cookies(result).get(authenticatorCookieName) mustBe None
  }

  "SocialAuthController.loginWithToken" should {

    "404 for nonexistent token" in new TestContext {
      running(app) {
        val nonExistentToken = IDs.next
        when(dataService.loginTokens.find(nonExistentToken)).thenReturn(Future.successful(None))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(nonExistentToken))).get
        status(result) mustBe NOT_FOUND
        assertNotJustLoggedIn(app, result)
        verify(dataService.loginTokens, times(1)).find(nonExistentToken)
      }
    }

    "Log in and redirect correctly for a valid token" in new TestContext {
      running(app) {
        val validToken = newLoginTokenFor(user)
        when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
        when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(user)))
        when(dataService.loginTokens.use(validToken)).thenReturn(Future.successful({}))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect)))).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(redirect)
        assertUserJustLoggedIn(app, user, result)
        verify(dataService.loginTokens, times(1)).find(validToken.value)
      }
    }

    "Don't log in and inform the user if invalid token" in new TestContext {
      running(app) {
        val invalidToken = newLoginTokenFor(user, isUsed = true)
        when(dataService.loginTokens.find(invalidToken.value)).thenReturn(Future.successful(Some(invalidToken)))
        val result = route(app, FakeRequest(controllers.routes.SocialAuthController.loginWithToken(invalidToken.value, Some(redirect)))).get
        status(result) mustBe OK
        contentAsString(result) must include("expired")
        assertNotJustLoggedIn(app, result)
        verify(dataService.loginTokens, times(1)).find(invalidToken.value)
      }
    }

    "Change logged-in user if necessary" in new TestContextWithLoggedInUser {
      running(app) {
        val initiallyLoggedOutUser = newUserFor(teamId)
        val validToken = newLoginTokenFor(initiallyLoggedOutUser)
        when(dataService.loginTokens.find(validToken.value)).thenReturn(Future.successful(Some(validToken)))
        when(dataService.users.find(validToken.userId)).thenReturn(Future.successful(Some(initiallyLoggedOutUser)))
        when(dataService.loginTokens.use(validToken)).thenReturn(Future.successful({}))
        val request = FakeRequest(controllers.routes.SocialAuthController.loginWithToken(validToken.value, Some(redirect))).withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(redirect)
        assertUserJustLoggedIn(app, initiallyLoggedOutUser, result)
        verify(dataService.loginTokens, times(1)).find(validToken.value)
      }
    }

    "Redirect correctly, ignore the token, don't log in if already logged in as correct user" in new TestContextWithLoggedInUser {
      running(app) {
        val alreadyUsedToken = newLoginTokenFor(user, isUsed = true)
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

  trait TestContext {

    def newAppFor(module: TestSilhouetteModule): Application = {
      GuiceApplicationBuilder().
        overrides(bind[DataService].to[MockDataService]).
        disable[SilhouetteModule].
        bindings(module).
        build()
    }
    lazy val redirect = "/whatever"
    lazy val teamId: String = IDs.next
    lazy val user: User = newUserFor(teamId)
    lazy val identities: Seq[(LoginInfo, User)] = Seq()
    lazy implicit val app: Application = {
      newAppFor(new TestSilhouetteModule {
        @Provides
        def provideEnvironment(): Environment[EllipsisEnv] = env
      })
    }
    lazy implicit val env: Environment[EllipsisEnv] = new FakeEnvironment[EllipsisEnv](identities)
    lazy val dataService = app.injector.instanceOf(classOf[DataService])

  }

  trait TestContextWithLoggedInUser extends TestContext {

    override lazy val identities = Seq(user.loginInfo -> user)

  }


}
