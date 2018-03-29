package json

case class SupportRequestConfig(
                                 containerId: String,
                                 csrfToken: Option[String],
                                 teamId: Option[String],
                                 user: Option[UserData]
                               )
