import java.time.OffsetDateTime

import json.SlackUserData
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import org.scalatestplus.play.PlaySpec

class SlackUserDataSpec extends PlaySpec {
  val botSlackTeam = "T1234567"
  val otherSlackTeam = "T8888888"
  val someEnterpriseId = Some("E123")
  val someOtherEnterpriseId = Some("E456")

  def botProfile(maybeEnterpriseId: Option[String] = None): SlackBotProfile = {
    SlackBotProfile(
      userId = "UMOCKBOT",
      teamId = IDs.next,
      maybeSlackEnterpriseId = maybeEnterpriseId,
      slackTeamId = botSlackTeam,
      token = IDs.next,
      createdAt = OffsetDateTime.now,
      allowShortcutMention = true
    )
  }

  def slackUser(teamId: String, maybeEnterpriseId: Option[String] = None, isBot: Boolean = false): SlackUserData = {
    SlackUserData(
      accountId = IDs.next,
      accountEnterpriseId = maybeEnterpriseId,
      accountTeamIds = SlackUserTeamIds(teamId),
      accountName = "A User",
      isPrimaryOwner = false,
      isOwner = false,
      isRestricted = false,
      isUltraRestricted = false,
      isBot = isBot,
      tz = Some("America/Toronto"),
      deleted = false,
      profile = None
    )
  }

  "canTriggerBot" should {
    "be true if a user is on the same team as the bot" in {
      val profile = botProfile()
      val user = slackUser(botSlackTeam)
      user.canTriggerBot(profile, None) mustBe true
    }

    "be true if a user is on the same grid as the bot" in {
      val profile = botProfile(someEnterpriseId)
      val user = slackUser(otherSlackTeam, someEnterpriseId)
      user.canTriggerBot(profile, someEnterpriseId) mustBe true
    }

    "be true if a user is an admin" in {
      val profile = botProfile()
      val user = slackUser(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
      user.canTriggerBot(profile, None) mustBe true
    }

    "be false if a user is on a different team" in {
      val profile = botProfile()
      val user = slackUser(otherSlackTeam)
      user.canTriggerBot(profile, None) mustBe false
    }

    "be false if a user is a bot on the same team" in {
      val profile = botProfile()
      val user = slackUser(botSlackTeam, isBot = true)
      user.canTriggerBot(profile, None) mustBe false
    }

    "be false if a user is a bot on the same grid" in {
      val profile = botProfile(someEnterpriseId)
      val user = slackUser(otherSlackTeam, someEnterpriseId, isBot = true)
      user.canTriggerBot(profile, someEnterpriseId) mustBe false
    }

    "be false if a user is on a different team on a different grid" in {
      val profile = botProfile(someEnterpriseId)
      val user = slackUser(otherSlackTeam, someOtherEnterpriseId)
      user.canTriggerBot(profile, someEnterpriseId) mustBe false
    }
  }
}
