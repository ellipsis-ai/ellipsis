package json

case class SlackUserProfileData(
                                 displayName: String,
                                 profile: SlackUserProfileNameData,
                                 isPrimaryOwner: Boolean,
                                 isOwner: Boolean,
                                 isRestricted: Boolean,
                                 isUltraRestricted: Boolean,
                                 tz: Option[String]
                               )

case class SlackUserProfileNameData(
                                     displayName: String,
                                     firstName: Option[String],
                                     lastName: Option[String],
                                     realName: Option[String]
                                   )
