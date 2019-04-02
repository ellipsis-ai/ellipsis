package controllers.api.json

import models.behaviors.ActionArg

case class AddMessageListenerInfo(
                                   actionName: String,
                                   arguments: Seq[ActionArg],
                                   userId: String,
                                   medium: String,
                                   channel: String,
                                   threadId: Option[String],
                                   token: String
                                 ) extends ApiMethodInfo
