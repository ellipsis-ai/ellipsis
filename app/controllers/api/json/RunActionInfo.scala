package controllers.api.json

import models.behaviors.ActionArg

case class RunActionInfo(
                          actionName: Option[String],
                          trigger: Option[String],
                          arguments: Seq[ActionArg],
                          responseContext: String,
                          maybeChannel: Option[String],
                          token: String,
                          originalEventType: Option[String],
                          originalMessageId: Option[String],
                          originalMessageThreadId: Option[String]
                        ) extends ApiMethodWithActionAndArgumentsInfo
