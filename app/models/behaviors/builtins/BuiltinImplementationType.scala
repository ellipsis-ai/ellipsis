package models.behaviors.builtins

import json.BehaviorGroupData
import models.team.Team
import services.DataService

trait BuiltinImplementationType {
  def addToGroupData(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData
}
