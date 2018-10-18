package json

import models.accounts.slack.SlackUserTeamIds

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
}
