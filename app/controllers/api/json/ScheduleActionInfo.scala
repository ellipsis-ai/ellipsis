package controllers.api.json

import models.behaviors.ActionArg

case class ScheduleActionInfo(
                               actionName: Option[String],
                               trigger: Option[String],
                               arguments: Seq[ActionArg],
                               recurrenceString: String,
                               useDM: Boolean,
                               channel: String,
                               token: String
                             ) extends ApiMethodWithActionAndArgumentsInfo
