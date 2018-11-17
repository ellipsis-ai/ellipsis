package models.behaviors.events.ms_teams

import com.fasterxml.jackson.core.JsonParseException
import models.behaviors.events.MessageActionButton
import play.api.libs.json.{JsString, JsValue, Json}
import services.ms_teams.apiModels.{ActionSubmit, CardElement}

case class MSTeamsMessageActionButton(
                                       text: String,
                                       value: String,
                                       maybeStyle: Option[String] = None
                                     ) extends MSTeamsMessageAction with MessageActionButton {

  private def jsonValue: JsValue = {
    try {
      Json.parse(value)
    } catch {
      case e: JsonParseException => JsString(value)
    }
  }

  def bodyElements: Seq[CardElement] = Seq()
  def actionElements: Seq[CardElement] = Seq(ActionSubmit(text, jsonValue))
}
