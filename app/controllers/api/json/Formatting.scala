package controllers.api.json

import json.Formatting._
import play.api.libs.json.Json

object Formatting {

  implicit val runActionInfoWrites = Json.writes[RunActionInfo]

  implicit val scheduleActionInfoWrites = Json.writes[ScheduleActionInfo]

  implicit val unscheduleActionInfoWrites = Json.writes[UnscheduleActionInfo]

  implicit val addMessageListenerInfoWrites = Json.writes[AddMessageListenerInfo]

  implicit val scheduleActionResultFormat = Json.format[ScheduleActionResult]
  implicit val scheduleResultFormat = Json.format[ScheduleResult]

  implicit val postMessageInfoWrites = Json.writes[PostMessageInfo]

  implicit val sayInfoWrites = Json.writes[SayInfo]

  implicit val generateApiTokenInfoWrites = Json.writes[GenerateApiTokenInfo]

}
