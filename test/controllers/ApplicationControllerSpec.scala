package controllers

import java.time.{OffsetDateTime, ZoneId}

import com.mohiva.play.silhouette.test._
import json.{BehaviorGroupData, TeamTimeZoneData}
import models.IDs
import models.accounts.user.UserTeamAccess
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.{ControllerTestContextWithLoggedInUser, NotFoundForOtherTeamContext}

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
        val behaviorGroupVersion = BehaviorGroupVersion(groupVersionId, behaviorGroup, groupName, None, None, None, None, OffsetDateTime.now)
        val behaviorVersion = BehaviorVersion(IDs.next, behavior, behaviorGroupVersion, None, None, None, None, false, None, OffsetDateTime.now)
        val teamAccess = mock[UserTeamAccess]

        when(dataService.users.teamAccessFor(user, Some(team.id))).thenReturn(Future.successful(teamAccess))
        when(teamAccess.maybeTargetTeam).thenReturn(Some(team))
        when(dataService.behaviorGroups.allFor(team)).thenReturn(Future.successful(Seq(behaviorGroup)))
        when(dataService.behaviorGroups.find(groupId, user)).thenReturn(Future.successful(Some(behaviorGroup)))
        when(dataService.behaviorGroupVersions.findWithoutAccessCheck(groupVersionId)).thenReturn(Future.successful(Some(behaviorGroupVersion)))
        when(dataService.behaviors.allForGroup(behaviorGroup)).thenReturn(Future.successful(Seq(behavior)))
        when(dataService.inputs.allForGroupVersion(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.libraries.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.nodeModuleVersions.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.behaviors.find(behaviorId, user)).thenReturn(Future.successful(Some(behavior)))
        when(dataService.behaviorVersions.findFor(behavior, behaviorGroupVersion)).thenReturn(Future.successful(Some(behaviorVersion)))
        when(dataService.behaviorParameters.allFor(behaviorVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.messageTriggers.allFor(behaviorVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.requiredAWSConfigs.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.requiredOAuth2ApiConfigs.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.requiredSimpleTokenApis.allFor(behaviorGroupVersion)).thenReturn(Future.successful(Seq()))
        when(dataService.teamEnvironmentVariables.lookForInCode(anyString)).thenReturn(Seq())
        when(dataService.dataTypeConfigs.maybeFor(behaviorVersion)).thenReturn(Future.successful(None))
        when(githubService.execute(anyString, any[JsValue])).thenReturn {
          Future.successful {
            Json.parse(
              """
                |{
                |  "data": {
                |    "repository": {
                |      "object": {
                |        "entries": []
                |      }
                |    }
                |  }
                |}
                |""".stripMargin)
          }
        }

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

  "setTeamTimeZone" should {
    "set the team time zone when passed a valid time zone name" in new ControllerTestContextWithLoggedInUser {
      running(app) {
        val teamAccess = mock[UserTeamAccess]
        val tz = ZoneId.of("America/Toronto")
        when(dataService.users.teamAccessFor(user, Some(team.id))).thenReturn(Future.successful(teamAccess))
        when(teamAccess.maybeTargetTeam).thenReturn(Some(team))
        when(dataService.teams.setTimeZoneFor(anyObject(), any[ZoneId])).thenReturn(Future(team.copy(maybeTimeZone = Some(tz))))

        val csrfToken = csrfProvider.generateToken
        val request = FakeRequest(controllers.routes.ApplicationController.setTeamTimeZone()).
          withSession(csrfConfig.tokenName -> csrfToken).
          withHeaders(csrfConfig.headerName -> csrfToken).
          withJsonBody(Json.obj("tzName" -> "America/Toronto")).
          withAuthenticator(user.loginInfo)
        val result = route(app, request).get
        status(result) mustBe OK
        import json.Formatting._
        contentAsJson(result).validate[TeamTimeZoneData] match {
          case JsSuccess(data, jsPath) => {
            data.tzName mustBe "America/Toronto"
          }
          case e: JsError => assert(false, "Not valid time zone data")
        }
      }
    }
  }

  "index" should {

    "show custom not found page when the wrong teamId supplied" in new NotFoundForOtherTeamContext {

      def buildCall: Call = controllers.routes.ApplicationController.index(Some(otherTeam.id))

      testNotFound

    }

  }

}
