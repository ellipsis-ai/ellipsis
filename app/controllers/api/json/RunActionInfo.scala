package controllers.api.json

case class RunActionInfo(
                          actionName: Option[String],
                          trigger: Option[String],
                          arguments: Seq[RunActionArgumentInfo],
                          responseContext: String,
                          maybeChannel: Option[String],
                          token: String,
                          originalEventType: Option[String],
                          originalMessageId: Option[String],
                          originalMessageThreadId: Option[String]
                        ) extends ApiMethodWithActionAndArgumentsInfo
