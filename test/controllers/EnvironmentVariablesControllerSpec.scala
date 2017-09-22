package controllers

import com.mohiva.play.silhouette.test._
import models.IDs
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.{ControllerTestContextWithLoggedInUser, NotFoundForOtherTeamContext}

import scala.concurrent.Future

class EnvironmentVariablesControllerSpec extends PlaySpec with MockitoSugar {

  "EnvironmentVariablesController.delete" should {

    "404 for nonexistent env var" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val nonExistentEnvVarName = IDs.next
        when(dataService.teams.find(team.id)).thenReturn(Future.successful(Some(team)))
        when(dataService.teamEnvironmentVariables.deleteFor(nonExistentEnvVarName, team)).thenReturn(Future.successful(false))
        val csrfToken = csrfProvider.generateToken
        val request =
          FakeRequest(controllers.routes.EnvironmentVariablesController.delete()).
            withSession(csrfConfig.tokenName -> csrfToken).
            withHeaders(csrfConfig.headerName -> csrfToken).
            withFormUrlEncodedBody("name" -> nonExistentEnvVarName).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
        verify(dataService.teamEnvironmentVariables, times(1)).deleteFor(nonExistentEnvVarName, team)
      }
    }

    "delete an env var" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val existingEnvVarName = IDs.next
        when(dataService.teams.find(team.id)).thenReturn(Future.successful(Some(team)))
        when(dataService.teamEnvironmentVariables.deleteFor(existingEnvVarName, team)).thenReturn(Future.successful(true))
        val csrfToken = csrfProvider.generateToken
        val request =
          FakeRequest(controllers.routes.EnvironmentVariablesController.delete()).
            withSession(csrfConfig.tokenName -> csrfToken).
            withHeaders(csrfConfig.headerName -> csrfToken).
            withFormUrlEncodedBody("name" -> existingEnvVarName).
            withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe OK
        verify(dataService.teamEnvironmentVariables, times(1)).deleteFor(existingEnvVarName, team)
      }
    }

    "EnvironmentVariablesController.list" should {

      "show custom not found page when the wrong teamId supplied" in new NotFoundForOtherTeamContext {

        def buildCall: Call = controllers.routes.EnvironmentVariablesController.list(Some(otherTeam.id))

        testNotFound

      }

    }

  }

}
