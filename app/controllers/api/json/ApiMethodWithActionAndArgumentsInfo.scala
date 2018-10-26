package controllers.api.json

trait ApiMethodWithActionAndArgumentsInfo extends ApiMethodWithActionInfo {
  val arguments: Seq[RunActionArgumentInfo]

  val argumentsMap: Map[String, String] = {
    arguments.map { ea =>
      (ea.name, ea.value)
    }.toMap
  }
}
