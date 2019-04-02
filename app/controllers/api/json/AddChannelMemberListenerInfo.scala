package controllers.api.json

case class AddChannelMemberListenerInfo(
                                         actionName: String,
                                         arguments: Seq[RunActionArgumentInfo],
                                         userId: String,
                                         medium: String,
                                         channel: String,
                                         token: String
                                       ) extends ApiMethodInfo {
  val argumentsMap: Map[String, String] = {
    arguments.map { ea =>
      (ea.name, ea.value)
    }.toMap
  }
}
