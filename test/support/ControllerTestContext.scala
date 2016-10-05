package support

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.crypto.{Base64AuthenticatorEncoder, CookieSigner}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import modules.{SilhouetteModule, TestSilhouetteModule}
import org.scalatest.MustMatchers
import play.api.Application
import play.api.inject._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import play.filters.csrf.CSRFConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ControllerTestContext extends TestContext with MustMatchers {

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

  def assertJustLoggedOut(app: Application, result: Future[Result]): Unit = {
    val authenticatorCookieName = app.configuration.getString("silhouette.authenticator.cookieName").get
    cookies(result).get(authenticatorCookieName).map { cookie =>
      //noinspection UnitInMap
      cookie.value must have length(0)
    }.getOrElse(assert(false, "Authenticator cookie must be present and empty"))
  }

  def newAppFor(testSilhouetteModule: TestSilhouetteModule): Application = {
    appBuilder.
      disable[SilhouetteModule].
      bindings(testSilhouetteModule).
      build()
  }
  lazy val redirect = "/whatever"
  lazy val identities: Seq[(LoginInfo, User)] = Seq()
  override lazy implicit val app: Application = {
    newAppFor(new TestSilhouetteModule {
      @Provides
      def provideEnvironment(): Environment[EllipsisEnv] = env
    })
  }
  lazy implicit val env: Environment[EllipsisEnv] = new FakeEnvironment[EllipsisEnv](identities)
  lazy val csrfProvider = app.injector.instanceOf(classOf[TokenProvider])
  lazy val csrfConfig = app.injector.instanceOf(classOf[CSRFConfig])

}
