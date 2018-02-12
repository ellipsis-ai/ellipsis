package json

case class SlackUserProfileData(
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
