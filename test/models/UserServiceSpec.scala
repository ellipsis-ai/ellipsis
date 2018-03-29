package models

import java.time.OffsetDateTime

import json.SlackUserData
import mocks.{MockAWSLambdaService, MockCacheService}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.botprofile.SlackBotProfile
import modules.ActorModule
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.{AWSLambdaService, GithubService, SlackEventService}
import services.caching.CacheService
import slack.api.SlackApiClient
import support.DBSpec

import scala.concurrent.Future

class UserServiceSpec extends DBSpec with MockitoSugar {

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[SlackEventService].toInstance(mock[SlackEventService])).
      overrides(bind[CacheService].to[MockCacheService]).
      disable[ActorModule].
      build()

//  "UserService.ensureUserFor" should {
//
//    "return an admin user if it exists, not creating a new user for another team" in {
//      withEmptyDB(dataService, { () =>
//        val slackTeamId = LinkedAccount.ELLIPSIS_SLACK_TEAM_ID
//        val team = newSavedTeam
//        val user = newSavedUserOn(team)
//        val linkedAccount = newSavedLinkedAccountFor(user, slackTeamId)
//        val loginInfo = linkedAccount.loginInfo
//
//        val otherTeam = newSavedTeam
//
//        val botProfile = runNow(dataService.slackBotProfiles.ensure(IDs.next, slackTeamId, IDs.next, IDs.next))
//        val client = SlackApiClient(botProfile.token)
//        when(slackEventService.clientFor(botProfile)).thenReturn(client)
//        val slackUserData = SlackUserData(linkedAccount.loginInfo.providerKey, slackTeamId, "", false, false, false, false, None, false, None)
//        when(slackEventService.maybeSlackUserDataFor(linkedAccount.loginInfo.providerKey, slackTeamId, client)).thenReturn(Future.successful(Some(slackUserData)))
//
//        //runNow(dataService.slackProfiles.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, loginInfo)))
//        runNow(dataService.users.ensureUserFor(loginInfo, otherTeam.id)) mustBe linkedAccount.user
//      })
//    }
//
//  }

//  "UserService.isAdmin" should {
//
//    "be false when there's no user data " in {
//      withEmptyDB(dataService, { () =>
//        val linkedAccount = newSavedLinkedAccount
//        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
//      })
//    }
//
//    "be false when the SlackProfile is for the wrong Slack team" in {
//      withEmptyDB(dataService, { () =>
//        val linkedAccount = newSavedLinkedAccount
//        val randomSlackTeamId = IDs.next
//        runNow(dataService.slackProfiles.save(SlackProfile(randomSlackTeamId, linkedAccount.loginInfo)))
//
//        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
//      })
//    }
//
//    "be true when there's a matching SlackProfile for the admin Slack team" in {
//      withEmptyDB(dataService, { () =>
//        val linkedAccount = newSavedLinkedAccount
//        runNow(dataService.slackProfiles.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, linkedAccount.loginInfo)))
//
//        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe true
//      })
//    }
//
//  }

}
