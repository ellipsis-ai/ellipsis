package json

case class SlackUserProfileData(
                                 name: String,
                                 profile: SlackUserProfileNameData,
                                 isPrimaryOwner: Boolean,
                                 isOwner: Boolean,
                                 isRestricted: Boolean,
                                 isUltraRestricted: Boolean,
                                 tz: Option[String]
                               )

case class SlackUserProfileNameData(
                                     firstName: Option[String],
                                     lastName: Option[String],
                                     realName: Option[String]
                                   )
