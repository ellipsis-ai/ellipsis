package controllers

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.test._
import json.BehaviorGroupData
import models.IDs
import models.accounts.user.UserTeamAccess
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerTestContextWithLoggedInUser

import scala.concurrent.Future

class ApplicationControllerSpec extends PlaySpec with MockitoSugar {

  "findBehaviorGroupsMatching" should {

    "respond with matching behaviorGroups" in new ControllerTestContextWithLoggedInUser {

      running(app) {
        val groupId = IDs.next
        val behaviorId = IDs.next
        val groupVersionId = IDs.next
        val groupName = "some skill"
        val behaviorGroup = BehaviorGroup(groupId, None, team, Some(groupVersionId), OffsetDateTime.now)
        val behavior = Behavior(behaviorId, team, Some(behaviorGroup), None, false, OffsetDateTime.now)
        val behaviorGroupVersion = BehaviorGroupVersion(groupVersionId, behaviorGroup, groupName, None, None, None, OffsetDateTime.now)
        val behaviorVersion = BehaviorVersion(IDs.next, behavior, behaviorGroupVersion, None, None, None, None, false, None, OffsetDateTime.now)
        val teamAccess = mock[UserTeamAccess]

        when(dataService.users.teamAccessFor(user, Some(team.id))).thenReturn(Future.successful(teamAccess))
        when(teamAccess.maybeTargetTeam).thenReturn(Some(team))
        when(dataService.behaviorGroups.allFor(team)).thenReturn(Future.successful(Seq(behaviorGroup)))
        when(dataService.behaviorGroups.find(groupId)).thenReturn(Future.successful(Some(behaviorGroup)))
        when(dataService.behaviorGroupVersions.findWithoutAccessCheck(groupVersionId)).thenReturn(Future.successful(Some(behaviorGroupVersion)))
        when(dataService.behaviors.allForGroup(behaviorGroup)).thenReturn(Future.successful(Seq(behavior)))
        when(dataService.inputs.allForGroupVersion(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.behaviors.find(behaviorId, user)).thenReturn(Future.successful(Some(behavior)))
        when(dataService.behaviorVersions.findFor(behavior, behaviorGroupVersion)).thenReturn(Future.successful(Some(behaviorVersion)))
        when(dataService.behaviorParameters.allFor(behaviorVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.messageTriggers.allFor(behaviorVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.awsConfigs.maybeFor(behaviorVersion)).thenReturn(Future.successful(None))
        when(dataService.requiredOAuth2ApiConfigs.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.requiredSimpleTokenApis.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.teamEnvironmentVariables.lookForInCode(anyString)).thenReturn(Seq())
        when(dataService.userEnvironmentVariables.lookForInCode(anyString)).thenReturn(Seq())
        when(githubService.publishedBehaviorGroupsFor(team, None)).thenReturn(Seq())

        val query = "some"
        val request = FakeRequest(controllers.routes.ApplicationController.findBehaviorGroupsMatching(query)).withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe OK
        import json.Formatting._
        contentAsJson(result).validate[Seq[BehaviorGroupData]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head.id.get mustBe groupId
          }
          case e: JsError => assert(false, "Not valid skill data")
        }
      }
    }

  }

}
