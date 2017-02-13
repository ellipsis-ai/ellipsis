package models.behaviors.input

import json.InputData
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team

import scala.concurrent.Future

trait InputService {

  def find(id: String): Future[Option[Input]]

  def createFor(data: InputData, team: Team): Future[Input]

  def ensureFor(data: InputData, team: Team): Future[Input]

  def allForGroup(group: BehaviorGroup): Future[Seq[Input]]

  def withEnsuredExportId(input: Input): Future[Input]

  def ensureExportIdsFor(behavior: Behavior): Future[Unit]

}
