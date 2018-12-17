package models

import json.SlackUserData
import mocks.{MockAWSLambdaService, MockCacheService}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackUserTeamIds
import modules.ActorModule
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.{AWSLambdaService, GithubService}
import services.caching.CacheService
import services.slack.{SlackApiClient, SlackEventService}
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


  "UserService.isAdmin" should {

    "be false when the slack account isn't from the admin team" in {
      withEmptyDB(dataService, { () =>
        val linkedAccount = newSavedLinkedAccount
        runNow(dataService.users.isAdmin(linkedAccount.user)) mustBe false
      })
    }

    "be true when there's a linked slack account on the admin team" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val adminSlackTeamId = LinkedAccount.ELLIPSIS_SLACK_TEAM_ID
        val linkedAccount = newSavedLinkedAccountFor(user)

        val botProfile = runNow(dataService.slackBotProfiles.ensure(IDs.next, adminSlackTeamId, IDs.next, IDs.next))
        val client = mock[SlackApiClient]
        when(slackEventService.clientFor(botProfile)).thenReturn(client)
        val slackUserData = SlackUserData(accountId = linkedAccount.loginInfo.providerKey, accountEnterpriseId = None, accountTeamIds = SlackUserTeamIds(adminSlackTeamId), accountName = "", isPrimaryOwner = false, isOwner = false, isRestricted = false, isUltraRestricted = false, isBot = false, tz = None, deleted = false, profile = None)
        when(slackEventService.maybeSlackUserDataFor(Matchers.eq(linkedAccount.loginInfo.providerKey), Matchers.eq(client), any())).thenReturn(Future.successful(Some(slackUserData)))

        runNow(dataService.users.isAdmin(linkedAccount.user)) mustBe true
      })
    }

  }

}
