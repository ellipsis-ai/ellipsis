package models

import play.api.libs.json.Json

sealed trait UINotification {
  val kind: String
}

case class EnvironmentVariableNotDefined(kind: String, environmentVariableName: String) extends UINotification

object UINotification {
  def environmentVariableNotDefined(environmentVariableName: String): EnvironmentVariableNotDefined = {
    EnvironmentVariableNotDefined("env_var_not_defined", environmentVariableName)
  }
}

object UINotificationFormatting {
  implicit val environmentVariableNotDefinedReads = Json.reads[EnvironmentVariableNotDefined]
  implicit val environmentVariableNotDefinedWrites = Json.writes[EnvironmentVariableNotDefined]
}
