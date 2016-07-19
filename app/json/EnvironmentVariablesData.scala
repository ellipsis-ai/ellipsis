package json

import models.EnvironmentVariable

case class EnvironmentVariableData(
                                    name: String,
                                    isAlreadySavedWithValue: Boolean,
                                    value: Option[String] // can be None even when there is a value when we want to hide it
                                    )

object EnvironmentVariableData {

  def withoutValueFor(variable: EnvironmentVariable): EnvironmentVariableData = {
    apply(variable.name, variable.value.trim.nonEmpty, None)
  }

  def withValueFor(variable: EnvironmentVariable): EnvironmentVariableData = {
    apply(variable.name, variable.value.trim.nonEmpty, Some(variable.value))
  }

}

case class EnvironmentVariablesData(
                                teamId: String,
                                variables: Seq[EnvironmentVariableData]
                                )
