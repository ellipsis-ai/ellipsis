package controllers

import com.mohiva.play.silhouette.test._
import models.IDs
import models.accounts.oauth1api.OAuth1Api
import models.accounts.oauth1application.OAuth1Application
import models.accounts.oauth2api.{AuthorizationCode, OAuth2Api}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.UserTeamAccess
import models.team.Team
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContextWithLoggedInUser

import scala.concurrent.Future

class IntegrationsControllerSpec extends PlaySpec with MockitoSugar {

  "IntegrationsController.edit" should {

    "404 for application from another team (even if it's shared)" in new MyContext {
      running(app) {
        val someOtherTeam = Team("Team1")
        val oauth1AppForOtherTeam = OAuth1Application(IDs.next, "", oauth1Api, IDs.next, IDs.next, someOtherTeam.id, isShared = true)
        val oauth2AppForOtherTeam = OAuth2Application(IDs.next, "", oauth2Api, IDs.next, IDs.next, None, someOtherTeam.id, isShared = true)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
        when(dataService.users.teamAccessFor(user, None)).thenReturn(Future.successful(teamAccess))
        when(dataService.users.isAdmin(user)).thenReturn(Future.successful(false))
        when(dataService.oauth1Apis.allFor(teamAccess.maybeTargetTeam)).thenReturn(Future.successful(Seq(oauth1Api)))
        when(dataService.oauth1Applications.find(oauth2AppForOtherTeam.id)).thenReturn(Future.successful(Some(oauth1AppForOtherTeam)))
        when(dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)).thenReturn(Future.successful(Seq(oauth2Api)))
        when(dataService.oauth2Applications.find(oauth2AppForOtherTeam.id)).thenReturn(Future.successful(Some(oauth2AppForOtherTeam)))
        val request =
          FakeRequest(controllers.web.settings.routes.IntegrationsController.edit(oauth2AppForOtherTeam.id)).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
        verify(dataService.oauth2Applications, times(1)).find(oauth2AppForOtherTeam.id)
      }
    }

  }

  trait MyContext extends ControllerTestContextWithLoggedInUser {

    lazy val authorizationUrl = "https://authorize.me/oauth2/authorize"
    lazy val oauth1ApiId = IDs.next
    lazy val oauth1Api = OAuth1Api(oauth1ApiId, "", "", "", authorizationUrl, None, None)
    lazy val oauth2ApiId = IDs.next
    lazy val oauth2Api = OAuth2Api(oauth2ApiId, "", AuthorizationCode, Some(authorizationUrl), "", None, None, None)

  }

}
