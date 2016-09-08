package support

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.crypto.{Base64AuthenticatorEncoder, CookieSigner}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import mocks.MockDataService
import models.IDs
import models.accounts.user.User
import models.silhouette.EllipsisEnv
import models.team.Team
import modules.{ActorModule, SilhouetteModule, TestSilhouetteModule}
import org.scalatest.MustMatchers
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import play.filters.csrf.CSRFConfig
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ControllerTestContext extends MustMatchers {

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

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)

  def newAppFor(testSilhouetteModule: TestSilhouetteModule): Application = {
    GuiceApplicationBuilder().
      overrides(bind[DataService].to[MockDataService]).
      disable[SilhouetteModule].
      disable[ActorModule].
      bindings(testSilhouetteModule).
      build()
  }
  lazy val redirect = "/whatever"
  lazy val teamId: String = IDs.next
  lazy val team: Team = Team(teamId, "")
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
  lazy val csrfProvider = app.injector.instanceOf(classOf[TokenProvider])
  lazy val csrfConfig = app.injector.instanceOf(classOf[CSRFConfig])

}
