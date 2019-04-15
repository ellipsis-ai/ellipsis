package controllers

import java.time.{OffsetDateTime, ZoneId}

import com.mohiva.play.silhouette.test._
import json.{BehaviorConfig, BehaviorGroupData, BehaviorVersionData, TeamTimeZoneData}
import models.IDs
import models.accounts.user.UserTeamAccess
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.managedbehaviorgroup.ManagedBehaviorGroupInfo
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
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
        val behaviorGroup = BehaviorGroup(groupId, None, team, OffsetDateTime.now)
        val behavior = Behavior(behaviorId, team, Some(behaviorGroup), None, false, OffsetDateTime.now)
        val behaviorGroupVersion = BehaviorGroupVersion(groupVersionId, behaviorGroup, groupName, None, None, None, OffsetDateTime.now)
        val behaviorVersion = BehaviorVersion(IDs.next, behavior, behaviorGroupVersion, None, None, None, None, responseType = Normal, canBeMemoized = false, isTest = false, OffsetDateTime.now)
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)

        when(dataService.behaviorGroups.maybeDataFor(groupId, user)).thenReturn({
          val behaviorVersionData = BehaviorVersionData(
            Some(behaviorVersion.id),
            team.id,
            Some(behaviorId),
            Some(groupId),
            None,
            None,
            None,
            "",
            "",
            Seq(),
            Seq(),
            BehaviorConfig(None, None, Normal.toString, None, false, None, None),
            None,
            Some(OffsetDateTime.now)
          )
          Future.successful(Some(
            BehaviorGroupData(
              Some(groupId),
              team.id,
              Some(groupName),
              None,
              None,
              Seq(),
              Seq(),
              Seq(behaviorVersionData),
              Seq(),
              Seq(),
              Seq(),
              Seq(),
              None,
              None,
              Some(OffsetDateTime.now),
              None,
              None,
              None,
              false,
              None,
              None
            )))
        })
        when(dataService.users.teamAccessFor(user, Some(team.id))).thenReturn(Future.successful(teamAccess))
        when(dataService.teams.find(team.id)).thenReturn(Future.successful(Some(team)))
        when(dataService.behaviorGroups.allFor(team)).thenReturn(Future.successful(Seq(behaviorGroup)))
        when(githubService.execute(anyString, any[JsValue])).thenReturn {
          Future.successful {
            Json.parse(
              """
                |{
                |  "data": {
                |    "repository": {
                |      "object": {
                |        "entries": [],
                |        "tree": {
                |          "entries": []
                |        }
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
        val teamAccess = UserTeamAccess(user, team, Some(team), Some("TestBot"), isAdminAccess = false)
        val tz = ZoneId.of("America/Toronto")
        when(dataService.users.teamAccessFor(user, Some(team.id))).thenReturn(Future.successful(teamAccess))
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
