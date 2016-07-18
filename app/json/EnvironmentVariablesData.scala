package json

case class EnvironmentVariableData(name: String, value: String)

case class EnvironmentVariablesData(
                                teamId: String,
                                variables: Seq[EnvironmentVariableData]
                                )
