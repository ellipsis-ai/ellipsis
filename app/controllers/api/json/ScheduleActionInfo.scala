package controllers.api.json

case class ScheduleActionInfo(
                               actionName: Option[String],
                               trigger: Option[String],
                               arguments: Seq[RunActionArgumentInfo],
                               recurrenceString: String,
                               useDM: Boolean,
                               channel: String,
                               token: String
                             ) extends ApiMethodWithActionAndArgumentsInfo
