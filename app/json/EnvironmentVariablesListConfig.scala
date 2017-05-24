package json

case class EnvironmentVariablesListConfig(
  containerId: String,
  csrfToken: Option[String],
  data: EnvironmentVariablesData
)
