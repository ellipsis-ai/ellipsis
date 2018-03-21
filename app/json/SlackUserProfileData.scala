package json

case class SlackUserProfileData(
                                 private val displayName: Option[String],
                                 firstName: Option[String],
                                 lastName: Option[String],
                                 realName: Option[String]
                               ) {
  def maybeDisplayName: Option[String] = {
    displayName.filter(_.nonEmpty).orElse {
      realName.filter(_.nonEmpty)
    }
  }
}
