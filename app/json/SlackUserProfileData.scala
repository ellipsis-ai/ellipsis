package json

case class SlackUserProfileData(
                                 profile: SlackUserProfileNameData,
                                 isPrimaryOwner: Boolean,
                                 isOwner: Boolean,
                                 isRestricted: Boolean,
                                 isUltraRestricted: Boolean,
                                 tz: Option[String]
                               )

case class SlackUserProfileNameData(
                                     // Display name can be empty, so it can't be depended on
                                     private val displayName: String,
                                     firstName: Option[String],
                                     lastName: Option[String],
                                     realName: Option[String]
                                   ) {
  def maybeDisplayName: Option[String] = {
    Option(displayName).filter(_.nonEmpty).orElse {
      realName.filter(_.nonEmpty)
    }
  }
}
