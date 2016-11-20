package models.behaviors.behaviorgroup

import models.team.Team
import org.joda.time.DateTime

case class BehaviorGroup(id: String, name: String, team: Team, createdAt: DateTime)
