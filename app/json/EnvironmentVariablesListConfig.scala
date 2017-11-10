package json

case class EnvironmentVariablesListConfig(
  containerId: String,
  csrfToken: Option[String],
  isAdmin: Boolean,
  data: EnvironmentVariablesData,
  focus: Option[String]
)
