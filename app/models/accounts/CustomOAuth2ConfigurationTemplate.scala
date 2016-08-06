package models.accounts

case class CustomOAuth2ConfigurationTemplate(
                                              name: String,
                                              authorizationUrl: String,
                                              accessTokenUrl: String,
                                              getProfileUrl: String,
                                              getProfileJsonPath: String
                                              )

object CustomOAuth2ConfigurationTemplate {

  val github = CustomOAuth2ConfigurationTemplate(
    "Github",
    "https://github.com/login/oauth/authorize",
    "https://github.com/login/oauth/access_token",
    "https://api.github.com/user?access_token=%s",
    "id"
  )

  val todoist = CustomOAuth2ConfigurationTemplate(
    "Todoist",
    "https://todoist.com/oauth/authorize",
    "https://todoist.com/oauth/access_token",
    """https://todoist.com/API/v7/sync?token=%s&resource_types=["user"]&sync_token="*"""",
    "user.id"
  )

  val all = Seq(
    github,
    todoist
  )

  def maybeFor(maybeTemplateName: Option[String]): Option[CustomOAuth2ConfigurationTemplate] = {
    maybeTemplateName.flatMap { templateName =>
      all.find(_.name == templateName)
    }
  }

}
