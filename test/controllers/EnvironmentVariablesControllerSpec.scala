package controllers

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test._
import models.IDs
import models.accounts.user.{User, UserTeamAccess}
import models.silhouette.EllipsisEnv
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.{CSRF, CSRFConfig}
import services.DataService
import support.{ControllerTestContextWithLoggedInUser, NotFoundForOtherTeamContext}

import scala.concurrent.Future

class EnvironmentVariablesControllerSpec extends PlaySpec with MockitoSugar {

  def setup(dataService: DataService, user: User, team: Team): Unit = {
    when(dataService.users.teamAccessFor(any[User], any[Option[String]])).thenReturn(Future.successful(
      UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
    ))
  }

  def newRequest(user: User, csrfProvider: CSRF.TokenProvider, csrfConfig: CSRFConfig)(implicit env: Environment[EllipsisEnv]) = {
    val csrfToken = csrfProvider.generateToken
    FakeRequest(controllers.web.settings.routes.EnvironmentVariablesController.delete()).
      withSession(csrfConfig.tokenName -> csrfToken).
      withHeaders(csrfConfig.headerName -> csrfToken).
      withAuthenticator(user.loginInfo)
  }

  "EnvironmentVariablesController.delete" should {

    "404 for nonexistent env var" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val nonExistentEnvVarName = IDs.next
        setup(dataService, user, team)
        when(dataService.teamEnvironmentVariables.deleteFor(nonExistentEnvVarName, team))
          .thenReturn(Future.successful(false))

        val legacyRequest = newRequest(user, csrfProvider, csrfConfig)
          .withFormUrlEncodedBody("name" -> nonExistentEnvVarName)
        val legacyResult = route(app, legacyRequest).get
        status(legacyResult) mustBe NOT_FOUND

        val modernRequest = newRequest(user, csrfProvider, csrfConfig).withFormUrlEncodedBody(
          "teamId" -> team.id,
          "name" -> nonExistentEnvVarName
        )
        val modernResult = route(app, modernRequest).get
        status(modernResult) mustBe NOT_FOUND

        verify(dataService.teamEnvironmentVariables, times(2)).deleteFor(nonExistentEnvVarName, team)
      }
    }

    "delete an env var" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val existingEnvVarName = IDs.next
        setup(dataService, user, team)
        when(dataService.teamEnvironmentVariables.deleteFor(existingEnvVarName, team))
          .thenReturn(Future.successful(true))
        val legacyRequest = newRequest(user, csrfProvider, csrfConfig).
            withFormUrlEncodedBody("name" -> existingEnvVarName)
        val legacyResult = route(app, legacyRequest).get
        status(legacyResult) mustBe OK

        val modernRequest = newRequest(user, csrfProvider, csrfConfig).withFormUrlEncodedBody(
          "teamId" -> team.id,
          "name" -> existingEnvVarName
        )
        val modernResult = route(app, modernRequest).get
        status(modernResult) mustBe OK

        verify(dataService.teamEnvironmentVariables, times(2)).deleteFor(existingEnvVarName, team)

      }
    }

  }

  "EnvironmentVariablesController.list" should {

    "show custom not found page when the wrong teamId supplied" in new NotFoundForOtherTeamContext {

      def buildCall: Call = controllers.web.settings.routes.EnvironmentVariablesController.list(Some(otherTeam.id))

      testNotFound

    }

  }

}
