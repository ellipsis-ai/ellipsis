package controllers.api.json

case class SayInfo(
                    message: String,
                    responseContext: String,
                    channel: String,
                    token: String,
                    originalEventType: Option[String],
                    originalMessageId: Option[String],
                    originalMessageThreadId: Option[String]
                  ) extends ApiMethodWithMessageInfo
