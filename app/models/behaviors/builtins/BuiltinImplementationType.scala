package models.behaviors.builtins

import json.BehaviorGroupData
import models.team.Team
import services.DataService

trait BuiltinImplementationType {
  def addToGroupDataTo(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData
}
