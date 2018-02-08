package models.loggedevent

import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}

case class CauseDetails(
                         messageText: Option[String],
                         triggerMatched: Option[String],
                         requestedBehaviorId: Option[String],
                         channelDetails: Option[ChannelDetails]
                       )

object CauseDetails {

  val empty: CauseDetails = CauseDetails(None, None, None, None)

  import Formatting._

  def fromJson(json: JsValue): CauseDetails = {
    json.validate[CauseDetails] match {
      case JsSuccess(obj, _) => obj
      case JsError(errors) => {
        Logger.error(s"Errors building CauseDetails from JSON: ${errors.mkString("\n")}}")
        empty
      }
    }
  }

}
