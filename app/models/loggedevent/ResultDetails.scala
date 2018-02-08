package models.loggedevent

import com.fasterxml.jackson.core.JsonParseException
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}

case class ResultDetails(
                         messageText: Option[String],
                         triggerMatched: Option[String],
                         behaviorId: Option[String],
                         channelDetails: Option[ChannelDetails]
                       )

object ResultDetails {

  val empty: ResultDetails = ResultDetails(None, None, None, None)

  import Formatting._

  def fromJsonString(str: String): ResultDetails = {
    try {
      Json.parse(str).validate[ResultDetails] match {
        case JsSuccess(obj, _) => obj
        case JsError(errors) => {
          Logger.error(s"Errors building ResultDetails from JSON: ${errors.mkString("\n")}}")
          empty
        }
      }
    } catch {
      case e: JsonParseException => {
        Logger.error(s"Errors parsing ResultDetails JSON: ${e.getMessage}}")
        empty
      }
    }
  }

}
