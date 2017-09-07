package models

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackProvider
import models.accounts.slack.profile.SlackProfile
import support.DBSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LinkedAccountServiceSpec extends DBSpec {

  def newSavedUserFor(teamId: String): Future[User] = {
    dataService.users.createFor(teamId)
  }

  def newSavedLinkedAccountFor(user: User): Future[LinkedAccount] = {
    val account = LinkedAccount(user, LoginInfo(SlackProvider.ID, IDs.next), OffsetDateTime.now)
    dataService.linkedAccounts.save(account)
  }

  def newSavedLinkedAccount: LinkedAccount = {
    runNow(for {
      user <- newSavedUserFor(newSavedTeam.id)
      linkedAccount <- newSavedLinkedAccountFor(user)
    } yield linkedAccount)
  }

  "LinkedAccountServiceSpec.isAdmin" should {

    "be false when there's no SlackProfile " in {
      withEmptyDB(dataService, { () =>
        val linkedAccount = newSavedLinkedAccount
        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      })
    }

    "be false when the SlackProfile is for the wrong Slack team" in {
      withEmptyDB(dataService, { () =>
        val linkedAccount = newSavedLinkedAccount
        val randomSlackTeamId = IDs.next
        runNow(dataService.slackProfiles.save(SlackProfile(randomSlackTeamId, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      })
    }

    "be true when there's a matching SlackProfile for the admin Slack team" in {
      withEmptyDB(dataService, { () =>
        val linkedAccount = newSavedLinkedAccount
        runNow(dataService.slackProfiles.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe true
      })
    }

  }

}
