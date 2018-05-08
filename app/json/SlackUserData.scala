package json

case class SlackUserData(
                          accountId: String,
                          accountTeamId: String,
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
}
