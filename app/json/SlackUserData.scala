package json

import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile

case class SlackUserData(
                          accountId: String,
                          accountEnterpriseId: Option[String],
                          accountTeamIds: SlackUserTeamIds,
                          accountName: String,
                          isPrimaryOwner: Boolean,
                          isOwner: Boolean,
                          isRestricted: Boolean,
                          isUltraRestricted: Boolean,
                          isBot: Boolean,
                          tz: Option[String],
                          deleted: Boolean,
                          profile: Option[SlackUserProfileData]
                        ) {
  def getDisplayName: String = {
    profile.flatMap(_.maybeDisplayName).getOrElse(accountName)
  }

  def maybeRealName: Option[String] = {
    profile.flatMap(_.realName)
  }

  def firstTeamId: String = accountTeamIds.head

  def canTriggerBot(botProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Boolean = {
    val isSameEnterpriseGrid = maybeEnterpriseId.isDefined && accountEnterpriseId == maybeEnterpriseId
    val userIsOnTeam = accountTeamIds.contains(botProfile.slackTeamId)
    val userIsAdmin = accountTeamIds.contains(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
    !isBot && (isSameEnterpriseGrid || userIsOnTeam || userIsAdmin)
  }
}
