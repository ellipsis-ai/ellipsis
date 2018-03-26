package models

import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.profile.SlackProfile
import support.DBSpec

class UserServiceSpec extends DBSpec {

  "UserServiceSpec.ensureUserFor" should {

    "return an admin user if it exists, not creating a new user for another team" in {
      withEmptyDB(dataService, { () =>
        val linkedAccount = newSavedLinkedAccount
        val loginInfo = linkedAccount.loginInfo

        val otherTeam = newSavedTeam

        runNow(dataService.slackProfiles.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, loginInfo)))
        runNow(dataService.users.ensureUserFor(loginInfo, otherTeam.id)) mustBe linkedAccount.user
      })
    }

  }

}
