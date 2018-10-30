package controllers.api.json

case class PostMessageInfo(
                            message: String,
                            responseContext: String,
                            channel: String,
                            token: String,
                            originalEventType: Option[String]
                          ) extends ApiMethodWithMessageInfo
