package models.behaviors.events.ms_teams

import models.behaviors.events.MessageMenu
import play.api.libs.json.Json
import services.ms_teams.apiModels._

case class MSTeamsMessageTextInput(name: String) extends MSTeamsMessageAction with MessageMenu {
  def bodyElements: Seq[CardElement] = Seq(InputText(name))
  def actionElements: Seq[CardElement] = Seq(ActionSubmit("Send response", Json.obj("actionName" -> name)))
}
