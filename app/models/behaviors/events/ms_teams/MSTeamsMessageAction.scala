package models.behaviors.events.ms_teams

import services.ms_teams.apiModels.CardAction

trait MSTeamsMessageAction {
  val cardAction: CardAction
}
