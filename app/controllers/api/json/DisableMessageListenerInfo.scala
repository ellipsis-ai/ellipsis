package controllers.api.json

case class DisableMessageListenerInfo(
                                       actionName: String,
                                       userId: String,
                                       medium: String,
                                       channel: String,
                                       threadId: Option[String],
                                       isForCoPilot: Option[Boolean],
                                       token: String
                                     ) extends ApiMethodInfo
