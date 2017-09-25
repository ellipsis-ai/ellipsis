package controllers

import models.IDs
import models.accounts.user.UserTeamAccess
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Call
import support.{NotFoundForOtherTeamContext, NotFoundWithLoginContext}

import scala.concurrent.Future

class BehaviorEditorControllerSpec extends PlaySpec with MockitoSugar {

  "newGroup" should {

    "show custom not found page when the wrong teamId supplied" in new NotFoundForOtherTeamContext {

      def buildCall: Call = controllers.routes.BehaviorEditorController.newGroup(Some(otherTeam.id))

      testNotFound

    }

  }

  "edit" should {

    "show custom not found page when unknown skill ID supplied" in new NotFoundWithLoginContext {

      val unknownSkillId = IDs.next

      when(dataService.behaviorGroups.find(unknownSkillId, user)).thenReturn(Future.successful(None))

      def buildCall: Call = controllers.routes.BehaviorEditorController.edit(unknownSkillId)

      def mockTeamAccessFor(teamAccess: UserTeamAccess): Unit = {
        when(dataService.users.teamAccessFor(user, None)).thenReturn(Future.successful(teamAccess))
      }

      testNotFound

    }

  }

}
