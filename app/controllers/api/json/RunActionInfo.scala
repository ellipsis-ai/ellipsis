package controllers.api.json

case class RunActionInfo(
                          actionName: Option[String],
                          trigger: Option[String],
                          arguments: Seq[RunActionArgumentInfo],
                          responseContext: String,
                          channel: String,
                          token: String,
                          originalEventType: Option[String],
                          originalMessageId: Option[String]
                        ) extends ApiMethodWithActionAndArgumentsInfo
