package controllers

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.test._
import models.IDs
import models.accounts.oauth2api.{AuthorizationCode, OAuth2Api}
import models.accounts.oauth2application.OAuth2Application
import models.team.Team
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContextWithLoggedInUser

import scala.concurrent.Future

class APIAccessControllerSpec extends PlaySpec with MockitoSugar {

  "APIAccessController.linkCustomOAuth2Service" should {

    "404 if the OAuth2 application doesn't exist" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val appId = IDs.next
        when(dataService.oauth2Applications.find(appId)).thenReturn(Future.successful(None))
        val request =
          FakeRequest(controllers.routes.APIAccessController.linkCustomOAuth2Service(appId, None, None, None, None)).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
      }
    }

    "Log user out and redirect to signin if not logged into the right team" in new TestContext {
      running(app) {
        val someOtherTeam = Team(IDs.next, "", None)
        val oauth2AppForOtherTeam = OAuth2Application(IDs.next, "", oauth2Api, IDs.next, IDs.next, None, someOtherTeam.id, isShared = false)
        when(dataService.oauth2Applications.find(oauth2AppForOtherTeam.id)).thenReturn(Future.successful(Some(oauth2AppForOtherTeam)))
        when(dataService.teams.find(someOtherTeam.id, user)).thenReturn(Future.successful(None))
        val request =
          FakeRequest(controllers.routes.APIAccessController.linkCustomOAuth2Service(oauth2AppForOtherTeam.id, None, None, None, None)).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe
          Some(
            routes.SocialAuthController.authenticateSlack(
              Some(request.uri),
              Some(someOtherTeam.id),
              None
            ).url
          )
        assertJustLoggedOut(app, result)
        verify(dataService.oauth2Applications, times(1)).find(oauth2AppForOtherTeam.id)
      }
    }

    "Proceed if logged into another team for shared oauth2 applications" in new TestContext {
      running(app) {
        val someOtherTeam = Team(IDs.next, "", None)
        val oauth2AppForOtherTeam = OAuth2Application(IDs.next, "", oauth2Api, IDs.next, IDs.next, None, someOtherTeam.id, isShared = true)
        when(dataService.oauth2Applications.find(oauth2AppForOtherTeam.id)).thenReturn(Future.successful(Some(oauth2AppForOtherTeam)))
        when(dataService.teams.find(someOtherTeam.id, user)).thenReturn(Future.successful(None))
        implicit val request =
          FakeRequest(controllers.routes.APIAccessController.linkCustomOAuth2Service(oauth2AppForOtherTeam.id, None, None, None, None)).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result).map { redirectLocation =>
          //noinspection UnitInMap
          redirectLocation must startWith(oauth2AppForOtherTeam.maybeAuthorizationUrl.get)
        }.getOrElse {
          assert(false, "Redirect location must contain authorization URL")
        }
        verify(dataService.oauth2Applications, times(1)).find(oauth2AppForOtherTeam.id)
      }
    }

    "Redirect to authorization URL if logged into the right team but on first step of OAuth dance" in new TestContext {
      running(app) {
        when(dataService.oauth2Applications.find(oauth2App.id)).thenReturn(Future.successful(Some(oauth2App)))
        when(dataService.teams.find(teamId, user)).thenReturn(Future.successful(Some(team)))
        implicit val request =
          FakeRequest(controllers.routes.APIAccessController.linkCustomOAuth2Service(oauth2App.id, None, None, None, None)).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe SEE_OTHER
        redirectLocation(result).map { redirectLocation =>
          //noinspection UnitInMap
          redirectLocation must startWith(oauth2App.maybeAuthorizationUrl.get)
        }.getOrElse {
          assert(false, "Redirect location must contain authorization URL")
        }
        verify(dataService.oauth2Applications, times(1)).find(oauth2App.id)
      }
    }

  }

  trait TestContext extends ControllerTestContextWithLoggedInUser {

    lazy val authorizationUrl = "https://authorize.me/oauth2/authorize"
    lazy val oauth2ApiId = IDs.next
    lazy val oauth2Api = OAuth2Api(oauth2ApiId, "", AuthorizationCode, Some(authorizationUrl), "", None, None, None)
    lazy val oauth2AppId = IDs.next
    lazy val oauth2App = OAuth2Application(oauth2AppId, "", oauth2Api, IDs.next, IDs.next, None, teamId, isShared = false)

  }

}
