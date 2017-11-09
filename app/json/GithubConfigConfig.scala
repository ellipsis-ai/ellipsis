package json

case class GithubConfigConfig(
                               containerId: String,
                               csrfToken: Option[String],
                               isAdmin: Boolean,
                               teamId: String,
                               linkedAccount: Option[LinkedAccountData]
                             )
