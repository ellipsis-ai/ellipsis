package models.behaviors.events.ms_teams

import models.behaviors.events.MessageAction
import services.ms_teams.apiModels.CardElement

trait MSTeamsMessageAction extends MessageAction {
  def bodyElements: Seq[CardElement]
  def actionElements: Seq[CardElement]
}
