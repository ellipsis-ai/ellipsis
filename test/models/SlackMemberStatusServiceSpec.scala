package models

import java.time.OffsetDateTime

import mocks.MockAWSLambdaService
import modules.ActorModule
import org.mockito.Matchers._
import org.mockito.Mockito.{never, verify, _}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.caching.CacheService
import services.slack.apiModels.MembershipData
import services.slack.{SlackApiClient, SlackApiService, SlackEventService}
import services.{AWSLambdaService, GithubService}
import support.DBSpec

import scala.concurrent.Future

class SlackMemberStatusServiceSpec extends DBSpec with MockitoSugar {

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[SlackEventService].toInstance(mock[SlackEventService])).
      overrides(bind[SlackApiService].toInstance(mock[SlackApiService])).
      overrides(bind[CacheService].toInstance(mock[CacheService])).
      disable[ActorModule].
      build()

  def newMembershipDataFor(slackUserId: String, slackTeamId: String, updatedAt: OffsetDateTime): MembershipData = {
    MembershipData(slackUserId, slackTeamId, IDs.next, updatedAt.toEpochSecond, deleted = false, is_bot = false, is_app_user = false)
  }


  "SlackMemberStatusService.updateAll" should {

    "do nothing if it has already run today" in {
      withEmptyDB(dataService, { () =>
        val slackUserId = IDs.next
        val slackTeamId = IDs.next
        val botProfile = newSavedSlackBotProfileFor(slackUserId, slackTeamId)

        val client = mock[SlackApiClient]
        when(slackEventService.clientFor(botProfile)).thenReturn(client)
        when(cacheService.get[OffsetDateTime](dataService.slackMemberStatuses.lastRunKey)).thenReturn(Future.successful(Some(OffsetDateTime.now)))
        runNow(dataService.slackMemberStatuses.updateAll)
        verify(client, never).allUsers()
      })
    }

    "create statuses based on Slack API response" in {
      withEmptyDB(dataService, { () =>
        val slackUserId = IDs.next
        val slackTeamId = IDs.next
        val botProfile = newSavedSlackBotProfileFor(slackUserId, slackTeamId)

        val membershipData = Seq(
          newMembershipDataFor(slackUserId, slackTeamId, OffsetDateTime.now.minusDays(50)),
          newMembershipDataFor(IDs.next, slackTeamId, OffsetDateTime.now.minusDays(100))
        )
        val client = mock[SlackApiClient]
        when(slackApiService.clientFor(botProfile)).thenReturn(client)
        when(client.allUsers(any[Option[String]])).thenReturn(Future.successful(membershipData))
        val key: String = dataService.slackMemberStatuses.lastRunKey
        when(cacheService.set(anyString, any, any)(any)).thenReturn(Future.successful({}))
        when(cacheService.get[OffsetDateTime](key)).thenReturn(Future.successful(None))
        runNow(dataService.slackMemberStatuses.updateAll)
        val statuses = runNow(dataService.slackMemberStatuses.allFor(slackTeamId))
        membershipData.foreach { data =>
          statuses.exists(ea => ea.slackUserId == data.id && ea.firstObservedAt.toEpochSecond == data.updated) mustBe(true)
        }
      })
    }

    "use linked account timestamp for first status, if present and linked account timestamp is earlier" in {
      withEmptyDB(dataService, { () =>
        val slackUserId = IDs.next
        val slackTeamId = IDs.next
        val botProfile = newSavedSlackBotProfileFor(slackUserId, slackTeamId)
        val team = runNow(dataService.teams.find(botProfile.teamId)).get
        val user = newSavedUserOn(team)

        val linkedAccount = newSavedLinkedAccountFor(user, slackUserId, Some(OffsetDateTime.now.minusDays(51)))
        val membershipData = Seq(
          newMembershipDataFor(slackUserId, slackTeamId, OffsetDateTime.now.minusDays(50))
        )
        val client = mock[SlackApiClient]
        when(slackApiService.clientFor(botProfile)).thenReturn(client)
        when(client.allUsers(any[Option[String]])).thenReturn(Future.successful(membershipData))
        val key: String = dataService.slackMemberStatuses.lastRunKey
        when(cacheService.set(anyString, any, any)(any)).thenReturn(Future.successful({}))
        when(cacheService.get[OffsetDateTime](key)).thenReturn(Future.successful(None))
        runNow(dataService.slackMemberStatuses.updateAll)
        val statuses = runNow(dataService.slackMemberStatuses.allFor(slackTeamId))
        membershipData.foreach { data =>
          statuses.exists(ea => ea.slackUserId == data.id && ea.firstObservedAt.toEpochSecond == linkedAccount.createdAt.toEpochSecond) mustBe(true)
        }
      })
    }

    "use membership data timestamp for first status even if linked account is present, if linked account timestamp is later" in {
      withEmptyDB(dataService, { () =>
        val slackUserId = IDs.next
        val slackTeamId = IDs.next
        val botProfile = newSavedSlackBotProfileFor(slackUserId, slackTeamId)
        val team = runNow(dataService.teams.find(botProfile.teamId)).get
        val user = newSavedUserOn(team)

        val linkedAccount = newSavedLinkedAccountFor(user, slackUserId, Some(OffsetDateTime.now.minusDays(49)))
        val membershipData = Seq(
          newMembershipDataFor(slackUserId, slackTeamId, OffsetDateTime.now.minusDays(50))
        )
        val client = mock[SlackApiClient]
        when(slackApiService.clientFor(botProfile)).thenReturn(client)
        when(client.allUsers(any[Option[String]])).thenReturn(Future.successful(membershipData))
        val key: String = dataService.slackMemberStatuses.lastRunKey
        when(cacheService.set(anyString, any, any)(any)).thenReturn(Future.successful({}))
        when(cacheService.get[OffsetDateTime](key)).thenReturn(Future.successful(None))
        runNow(dataService.slackMemberStatuses.updateAll)
        val statuses = runNow(dataService.slackMemberStatuses.allFor(slackTeamId))
        membershipData.foreach { data =>
          statuses.exists(ea => ea.slackUserId == data.id && ea.firstObservedAt.toEpochSecond == data.updated) mustBe(true)
        }
      })
    }

    "not create duplicate statuses" in {
      withEmptyDB(dataService, { () =>
        val slackUserId = IDs.next
        val slackTeamId = IDs.next
        val botProfile = newSavedSlackBotProfileFor(slackUserId, slackTeamId)

        val membershipData = Seq(
          newMembershipDataFor(slackUserId, slackTeamId, OffsetDateTime.now.minusDays(50)),
          newMembershipDataFor(IDs.next, slackTeamId, OffsetDateTime.now.minusDays(100))
        )
        val client = mock[SlackApiClient]
        when(slackApiService.clientFor(botProfile)).thenReturn(client)
        when(client.allUsers(any[Option[String]])).thenReturn(Future.successful(membershipData))
        val key: String = dataService.slackMemberStatuses.lastRunKey
        when(cacheService.set(anyString, any, any)(any)).thenReturn(Future.successful({}))
        when(cacheService.get[OffsetDateTime](key)).thenReturn(Future.successful(None))
        runNow(dataService.slackMemberStatuses.updateAll)
        verify(client, times(1)).allUsers()
        val statuses = runNow(dataService.slackMemberStatuses.allFor(slackTeamId))
        membershipData.foreach { data =>
          statuses.exists(ea => ea.slackUserId == data.id && ea.firstObservedAt.toEpochSecond == data.updated) mustBe(true)
        }

        runNow(dataService.slackMemberStatuses.updateAll)
        verify(client, times(2)).allUsers()
        val statusesAfterSecondUpdate = runNow(dataService.slackMemberStatuses.allFor(slackTeamId))
        statusesAfterSecondUpdate.sortBy(_.id) must equal(statuses.sortBy(_.id))
      })
    }

  }

}
