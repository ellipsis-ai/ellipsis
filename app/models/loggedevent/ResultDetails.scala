package models.loggedevent

import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}

case class ResultDetails(
                          responseText: Option[String],
                          invokedBehaviorVersionId: Option[String],
                          channelDetails: Option[ChannelDetails]
                       )

object ResultDetails {

  val empty: ResultDetails = ResultDetails(None, None, None)

  import Formatting._

  def fromJson(json: JsValue): ResultDetails = {
    json.validate[ResultDetails] match {
      case JsSuccess(obj, _) => obj
      case JsError(errors) => {
        Logger.error(s"Errors building ResultDetails from JSON: ${errors.mkString("\n")}}")
        empty
      }
    }
  }

}
