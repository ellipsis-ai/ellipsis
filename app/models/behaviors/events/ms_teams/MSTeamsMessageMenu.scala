package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageMenu, MessageMenuItem}
import play.api.libs.json.JsNull
import services.ms_teams.apiModels.{ActionSubmit, CardElement, InputChoice, InputChoiceSet}

case class MSTeamsMessageMenu(name: String, text: String, options: Seq[MSTeamsMessageMenuItem]) extends MSTeamsMessageAction with MessageMenu {
  private def choices = options.map(ea => InputChoice(ea.text, ea.value))
  def bodyElements: Seq[CardElement] = Seq(InputChoiceSet(name, text, choices))
  def actionElements: Seq[CardElement] = Seq(ActionSubmit(text, JsNull))
}

case class MSTeamsMessageMenuItem(text: String, value: String) extends MessageMenuItem
