package models.loggedevent

import com.fasterxml.jackson.core.JsonParseException
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}

case class CauseDetails(
                       messageText: Option[String],
                       triggerMatched: Option[String],
                       behaviorId: Option[String],
                       channelDetails: Option[ChannelDetails]
                       )

object CauseDetails {

  val empty: CauseDetails = CauseDetails(None, None, None, None)

  import Formatting._

  def fromJsonString(str: String): CauseDetails = {
    try {
      Json.parse(str).validate[CauseDetails] match {
        case JsSuccess(obj, _) => obj
        case JsError(errors) => {
          Logger.error(s"Errors building CauseDetails from JSON: ${errors.mkString("\n")}}")
          empty
        }
      }
    } catch {
      case e: JsonParseException => {
        Logger.error(s"Errors parsing CauseDetails JSON: ${e.getMessage}}")
        empty
      }
    }
  }

}
