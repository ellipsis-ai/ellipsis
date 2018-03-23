package models

import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.profile.SlackProfile
import support.DBSpec

class LinkedAccountServiceSpec extends DBSpec {

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
