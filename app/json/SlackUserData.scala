package json

import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import services.slack.apiModels.SlackUser

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

object SlackUserData {

  def fromSlackUser(slackUser: SlackUser, botProfile: SlackBotProfile): SlackUserData = {
    val maybeProfile = slackUser.profile.map { profile =>
      SlackUserProfileData(
        profile.display_name,
        profile.first_name,
        profile.last_name,
        profile.real_name,
        profile.email,
        profile.phone
      )
    }
    val maybeTeams = slackUser.enterprise_user.flatMap(_.teams)
    val firstTeam = slackUser.team_id.
      orElse(maybeTeams.flatMap(_.headOption)).
      getOrElse(botProfile.slackTeamId)
    val otherTeams = maybeTeams.filter(_ != firstTeam).getOrElse(Seq.empty)
    SlackUserData(
      slackUser.id,
      slackUser.enterprise_user.flatMap(_.enterprise_id),
      SlackUserTeamIds(firstTeam, otherTeams),
      slackUser.name,
      isPrimaryOwner = slackUser.is_primary_owner.getOrElse(false),
      isOwner = slackUser.is_owner.getOrElse(false),
      isRestricted = slackUser.is_restricted.getOrElse(false),
      isUltraRestricted = slackUser.is_ultra_restricted.getOrElse(false),
      isBot = slackUser.is_bot.getOrElse(false),
      tz = slackUser.tz,
      slackUser.deleted.getOrElse(false),
      maybeProfile
    )
  }
}
