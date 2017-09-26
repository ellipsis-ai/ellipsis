package support

import java.time.OffsetDateTime
import com.mohiva.play.silhouette.test._
import controllers.routes
import models.IDs
import models.accounts.user.UserTeamAccess
import models.team.Team
import org.mockito.Mockito.when
import play.api.http.MimeTypes
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

trait NotFoundWithLoginContext extends ControllerTestContextWithLoggedInUser {

  def buildCall: Call
  def mockTeamAccessFor(teamAccess: UserTeamAccess)

  def testNotFound = {
    running(app) {
      val teamAccess = mock[UserTeamAccess]
      val loggedInTeam = Team(IDs.next, "Logged in", None, OffsetDateTime.now)
      mockTeamAccessFor(teamAccess)
      when(teamAccess.maybeTargetTeam).thenReturn(None)
      when(teamAccess.loggedInTeam).thenReturn(loggedInTeam)
      when(teamAccess.maybeAdminAccessToTeam).thenReturn(None)
      when(teamAccess.maybeAdminAccessToTeamId).thenReturn(None)

      val request =
        FakeRequest(buildCall).
          withAuthenticator(user.loginInfo).
          withHeaders(("Accept", MimeTypes.HTML))
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND

      val expectedSignInLink = routes.SocialAuthController.authenticateSlack(
        Some(request.uri),
        None,
        None
      ).url

      val content = contentAsString(result)
      content must include(expectedSignInLink)
      content must include(loggedInTeam.name)
    }
  }
}

trait NotFoundForOtherTeamContext extends NotFoundWithLoginContext {
  val otherTeam: Team = Team(IDs.next, "Other team", None, OffsetDateTime.now)
  def mockTeamAccessFor(teamAccess: UserTeamAccess) = {
    when(dataService.users.teamAccessFor(user, Some(otherTeam.id))).thenReturn(Future.successful(teamAccess))
  }
}
