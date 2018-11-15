package models.behaviors.events.ms_teams

import models.behaviors.events.MessageAction
import services.ms_teams.apiModels.CardAction

trait MSTeamsMessageAction extends MessageAction {
  def cardAction: CardAction
}
