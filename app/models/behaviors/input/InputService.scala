package models.behaviors.input

import json.InputData
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team

import scala.concurrent.Future

trait InputService {

  def ensureFor(data: InputData, team: Team): Future[Input]

  def allFor(group: BehaviorGroup): Future[Seq[Input]]

  def changeGroup(input: Input, newGroup: BehaviorGroup): Future[Input]

}
