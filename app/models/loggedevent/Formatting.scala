package models.loggedevent

import play.api.libs.json.Json

object Formatting {

  lazy implicit val channelDetailsFormat = Json.format[ChannelDetails]
  lazy implicit val causeDetailsFormat = Json.format[CauseDetails]
  lazy implicit val resultDetailsFormat = Json.format[ResultDetails]

}
