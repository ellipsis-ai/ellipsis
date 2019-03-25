package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageMenu, MessageMenuItem}
import play.api.libs.json.Json
import services.ms_teams.apiModels.{ActionSubmit, CardElement, InputChoice, InputChoiceSet}

case class MSTeamsMessageMenu(name: String, text: String, options: Seq[MSTeamsMessageMenuItem]) extends MSTeamsMessageAction with MessageMenu {
  private def choices = options.map(ea => InputChoice(ea.text, ea.value))
  def bodyElements: Seq[CardElement] = choices.headOption.map { firstChoice =>
    Seq(InputChoiceSet(name, firstChoice.value, choices))
  }.getOrElse(Seq())
  def actionElements: Seq[CardElement] = Seq(ActionSubmit(text, Json.obj("actionName" -> name)))
}

case class MSTeamsMessageMenuItem(text: String, value: String) extends MessageMenuItem
