package models.behaviors.events.ms_teams

import play.api.libs.json.Json
import services.ms_teams.apiModels.CardAction

case class MSTeamsMessageActionButton(name: String, text: String, value: String, maybeStyle: Option[String] = None) extends MSTeamsMessageAction {

  lazy val cardAction: CardAction = CardAction("Action.Submit", text, Json.parse(value))
}
