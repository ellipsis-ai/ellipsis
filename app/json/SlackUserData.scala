package json

case class SlackUserData(
                          accountId: String,
                          accountTeamId: String,
                          accountName: String,
                          fullName: Option[String],
                          tz: Option[String],
                          deleted: Boolean,
                          profile: SlackUserProfileData
                        ) {
  def getDisplayName: String = {
    profile.profile.maybeDisplayName.getOrElse(accountName)
  }
}
