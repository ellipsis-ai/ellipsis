package models.behaviors.events.ms_teams

import com.fasterxml.jackson.core.JsonParseException
import models.behaviors.events.MessageActionButton
import play.api.libs.json.{JsString, JsValue, Json}
import services.ms_teams.apiModels.CardAction

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

  def cardAction: CardAction = CardAction("Action.Submit", text, jsonValue)
}
