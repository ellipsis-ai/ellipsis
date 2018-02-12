package json

case class SlackUserData(
                          accountId: String,
                          accountTeamId: String,
                          displayName: String,
                          fullName: Option[String],
                          tz: Option[String],
                          deleted: Boolean,
                          profile: SlackUserProfileData
                        )
